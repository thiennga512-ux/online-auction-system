package com.auction.server.service;

import java.sql.SQLException;
import java.util.Optional;

import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.enums.AuctionStatus;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.network.Response;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.network.UserConnectionManager;
import com.auction.server.observer.AuctionBroadcaster;
import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;

/**
 * ============================================================
 * AuctionLifecycleService — SRP: Vòng đời phiên đấu giá
 * ============================================================
 *
 * Chịu trách nhiệm DUY NHẤT: quản lý chuyển đổi trạng thái
 * vòng đời của phiên (OPEN → RUNNING → FINISHED) và xử lý
 * thanh toán/chuyển tiền khi phiên kết thúc.
 * ============================================================
 */
public class AuctionLifecycleService {

  private final AuctionSessionDAO auctionSessionDAO;
  private final BidDAO bidDAO;
  private final UserDAO userDAO;
  private final ItemService itemService;
  private final AuctionQueryService queryService;

  public AuctionLifecycleService(AuctionSessionDAO auctionSessionDAO, BidDAO bidDAO,
      UserDAO userDAO, ItemService itemService, AuctionQueryService queryService) {
    this.auctionSessionDAO = auctionSessionDAO;
    this.bidDAO = bidDAO;
    this.userDAO = userDAO;
    this.itemService = itemService;
    this.queryService = queryService;
  }

  /**
   * Chuyển phiên từ OPEN → RUNNING và broadcast sự kiện bắt đầu.
   *
   * @param sessionId ID phiên cần bắt đầu
   */
  public void startAuction(String sessionId) {
    AuctionSession session = queryService.getSessionOrThrow(sessionId);

    if (session.getStatus() != AuctionStatus.OPEN) {
      throw new IllegalStateException("Phiên không ở trạng thái OPEN: " + session.getStatus());
    }

    try {
      auctionSessionDAO.updateStatus(sessionId, AuctionStatus.RUNNING);

      // Tạo bid giá khởi điểm (INITIAL bid)
      Bid initialBid = Bid.createInitial(sessionId, session.getItem().getStartingPrice());
      bidDAO.save(initialBid);

      System.out.printf("[AuctionLifecycleService] Phiên %s đã BẮT ĐẦU! Giá khởi điểm: %.0f VND%n",
          sessionId.substring(0, 8), session.getItem().getStartingPrice());

      // Broadcast sự kiện BẮT ĐẦU tới các client đang xem phiên
      Dto.AuctionStartedEvent event = new Dto.AuctionStartedEvent(
          sessionId, session.getItem().getStartingPrice());
      Response response = Response.success(ActionType.AUCTION_STARTED_BROADCAST,
          "Phiên đấu giá đã bắt đầu!", event);
      AuctionBroadcaster.getInstance().broadcastToSession(sessionId, response);

      // Gửi thông báo chuông cho Seller và Bidder đã đăng ký
      NotificationService.getInstance().notifyAuctionStarted(session);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /**
   * Chuyển phiên từ RUNNING → FINISHED, xử lý thanh toán và broadcast kết thúc.
   *
   * @param sessionId ID phiên cần kết thúc
   */
  public void finishAuction(String sessionId) {
    AuctionSession session = queryService.getSessionOrThrow(sessionId);

    if (session.getStatus() != AuctionStatus.RUNNING) {
      throw new IllegalStateException("Phiên không đang RUNNING: " + session.getStatus());
    }

    try {
      auctionSessionDAO.updateStatus(sessionId, AuctionStatus.FINISHED);

      if (session.getCurrentWinnerId() != null) {
        handleAuctionWin(session);
      } else {
        itemService.markItemAsAvailable(session.getItem().getId());
        System.out.printf("[AuctionLifecycleService] Phiên %s kết thúc — KHÔNG có người thắng%n",
            sessionId.substring(0, 8));
      }

      // Broadcast sự kiện KẾT THÚC
      Dto.AuctionEndedEvent event = new Dto.AuctionEndedEvent(
          sessionId,
          session.getCurrentWinnerName() != null ? session.getCurrentWinnerName() : "Không có",
          session.getCurrentPrice());
      String msg = session.getCurrentWinnerId() != null
          ? "Phiên đấu giá đã kết thúc! Người thắng: " + session.getCurrentWinnerName()
          : "Phiên đấu giá đã kết thúc mà không có người thắng.";

      Response response = Response.success(ActionType.AUCTION_ENDED_BROADCAST, msg, event);
      AuctionBroadcaster.getInstance().broadcastToSession(sessionId, response);

      NotificationService.getInstance().notifyAuctionEnded(session);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database khi kết thúc phiên: " + e.getMessage(), e);
    }
  }

  /**
   * Xử lý khi phiên đấu giá có người thắng: chuyển tiền Seller, trừ tiền Bidder thắng.
   */
  private void handleAuctionWin(AuctionSession session) throws SQLException {
    double finalPrice = session.getCurrentPrice();
    double commission = finalPrice * 0.05; // 5% hoa hồng
    double sellerReceives = finalPrice - commission;

    itemService.markItemAsSold(session.getItem().getId());

    // Chuyển tiền cho Seller (trừ hoa hồng)
    Optional<User> sellerOpt = userDAO.findById(session.getSellerId());
    if (sellerOpt.isPresent() && sellerOpt.get() instanceof Seller seller) {
      seller.setBalance(seller.getBalance() + sellerReceives);
      userDAO.updateSellerDetails(seller);

      // Cập nhật số dư realtime cho Seller
      com.auction.server.network.ClientHandler sellerHandler =
          UserConnectionManager.getInstance().getHandler(seller.getId());
      if (sellerHandler != null) {
        Dto.DepositResultResponse result = new Dto.DepositResultResponse(seller.getBalance());
        sellerHandler.sendResponse(Response.success(ActionType.DEPOSIT_BALANCE,
            "Thanh toán thành công từ phiên đấu giá", result));
      }
    }

    // Trừ tiền đóng băng của người thắng
    Optional<User> winnerOpt = userDAO.findById(session.getCurrentWinnerId());
    if (winnerOpt.isPresent() && winnerOpt.get() instanceof Bidder winner) {
      winner.setFrozenBalance(winner.getFrozenBalance() - finalPrice);
      userDAO.updateBidderDetails(winner);

      // Cập nhật số dư realtime cho Bidder thắng
      com.auction.server.network.ClientHandler winnerHandler =
          UserConnectionManager.getInstance().getHandler(winner.getId());
      if (winnerHandler != null) {
        Dto.DepositResultResponse result = new Dto.DepositResultResponse(winner.getBalance());
        winnerHandler.sendResponse(Response.success(ActionType.DEPOSIT_BALANCE,
            "Thanh toán thành công phiên đấu giá", result));
      }
    }

    System.out.printf(
        "[AuctionLifecycleService] Phiên %s KẾT THÚC!%n"
            + "  Người thắng: %s%n"
            + "  Giá thắng: %.0f VND%n"
            + "  Hoa hồng: %.0f VND%n"
            + "  Seller nhận: %.0f VND%n",
        session.getId().substring(0, 8),
        session.getCurrentWinnerName(),
        finalPrice, commission, sellerReceives);
  }
}
