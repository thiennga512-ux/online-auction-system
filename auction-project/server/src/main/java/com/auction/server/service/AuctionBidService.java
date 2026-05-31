package com.auction.server.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.model.Bidder;
import com.auction.model.Item;
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
 * AuctionBidService — SRP: Xử lý đặt giá trong phiên đấu giá
 * ============================================================
 *
 * Chịu trách nhiệm DUY NHẤT: xử lý nghiệp vụ đặt giá (bid),
 * bao gồm kiểm tra hợp lệ, quản lý dòng tiền đóng băng/hoàn trả,
 * và phát sóng thông báo bid mới tới client theo thời gian thực.
 * ============================================================
 */
public class AuctionBidService {

  private final AuctionSessionDAO auctionSessionDAO;
  private final BidDAO bidDAO;
  private final UserDAO userDAO;
  private final AuctionQueryService queryService;

  // Lock theo sessionId để tránh race condition khi nhiều bid vào cùng lúc
  private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();

  public AuctionBidService(AuctionSessionDAO auctionSessionDAO, BidDAO bidDAO,
      UserDAO userDAO, AuctionQueryService queryService) {
    this.auctionSessionDAO = auctionSessionDAO;
    this.bidDAO = bidDAO;
    this.userDAO = userDAO;
    this.queryService = queryService;
  }

  /**
   * Xử lý đặt giá cho một phiên đấu giá. Thread-safe theo từng phiên (Lock Striping).
   *
   * @param bidder    Người đặt giá
   * @param sessionId ID phiên đấu giá
   * @param amount    Số tiền đặt giá
   * @param bidType   Loại bid (MANUAL / AUTO)
   * @return Bid vừa được tạo thành công
   */
  public Bid placeBid(Bidder bidder, String sessionId, double amount, Bid.BidType bidType) {
    Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());

    synchronized (lock) {
      // Bước 1: Lấy phiên từ DB để đảm bảo dữ liệu mới nhất
      AuctionSession session = queryService.getSessionOrThrow(sessionId);

      // Bước 2: Kiểm tra phiên có đang RUNNING không
      if (!session.getStatus().isAcceptingBids()) {
        throw new IllegalStateException(
            "Phiên không nhận bid. Trạng thái: " + session.getStatus().getDisplayName());
      }

      // Không cho phép Seller tự đấu giá sản phẩm của mình
      if (session.getSellerId().equals(bidder.getId())) {
        throw new IllegalArgumentException("Bạn không thể tự đấu giá sản phẩm của chính mình!");
      }

      // Bước 3: Kiểm tra giá và số dư
      Item item = session.getItem();
      double minRequired = session.getCurrentPrice() + item.getBidIncrement();
      if (amount < minRequired) {
        throw new IllegalArgumentException(
            String.format("Giá đặt %.0f VND không hợp lệ. Tối thiểu: %.0f VND",
                amount, minRequired));
      }

      String oldWinnerId = session.getCurrentWinnerId();
      double oldPrice = session.getCurrentPrice();
      boolean isSelfOutbid = oldWinnerId != null && oldWinnerId.equals(bidder.getId());

      // Nếu tự đè giá chính mình thì được dùng lại tiền đóng băng cũ
      double effectiveBalance = bidder.getBalance();
      if (isSelfOutbid) {
        effectiveBalance += oldPrice;
      }
      if (effectiveBalance < amount) {
        throw new IllegalArgumentException(
            "Số dư ký quỹ không đủ để đặt giá (Cần: " + String.format("%,.0f", amount) + " VND)");
      }

      // Bước 4: Tạo Bid mới
      Bid bid;
      if (bidType == Bid.BidType.AUTO) {
        bid = Bid.createAuto(sessionId, bidder.getId(), bidder.getFullName(), amount, amount);
      } else {
        bid = Bid.createManual(sessionId, bidder.getId(), bidder.getFullName(), amount);
      }

      try {
        // Bước 5a: Quản lý dòng tiền — Giải phóng tiền cho người bị vượt giá
        if (oldWinnerId != null) {
          if (isSelfOutbid) {
            // Tự đè giá chính mình — hoàn trả tiền đóng băng cũ
            bidder.setFrozenBalance(bidder.getFrozenBalance() - oldPrice);
            bidder.setBalance(bidder.getBalance() + oldPrice);
          } else {
            Optional<User> oldWinnerOpt = userDAO.findById(oldWinnerId);
            if (oldWinnerOpt.isPresent() && oldWinnerOpt.get() instanceof Bidder oldWinner) {
              oldWinner.setFrozenBalance(oldWinner.getFrozenBalance() - oldPrice);
              oldWinner.setBalance(oldWinner.getBalance() + oldPrice);
              userDAO.updateBidderDetails(oldWinner);

              // Cập nhật số dư realtime cho người bị vượt giá
              com.auction.server.network.ClientHandler oldWinnerHandler =
                  UserConnectionManager.getInstance().getHandler(oldWinner.getId());
              if (oldWinnerHandler != null) {
                Dto.DepositResultResponse result =
                    new Dto.DepositResultResponse(oldWinner.getBalance());
                oldWinnerHandler.sendResponse(Response.success(ActionType.DEPOSIT_BALANCE,
                    "Đã hoàn trả số dư đóng băng", result));
              }
            }
          }
        }

        // Bước 5b: Đóng băng tiền của người vừa đặt giá mới
        bidder.setBalance(bidder.getBalance() - amount);
        bidder.setFrozenBalance(bidder.getFrozenBalance() + amount);
        userDAO.updateBidderDetails(bidder);

        // Cập nhật số dư realtime cho người đặt giá
        com.auction.server.network.ClientHandler bidderHandler =
            UserConnectionManager.getInstance().getHandler(bidder.getId());
        if (bidderHandler != null) {
          Dto.DepositResultResponse result = new Dto.DepositResultResponse(bidder.getBalance());
          bidderHandler.sendResponse(Response.success(ActionType.DEPOSIT_BALANCE,
              "Đã đóng băng số dư", result));
        }

        // Bước 5c: Lưu bid và cập nhật trạng thái phiên
        bidDAO.save(bid);
        auctionSessionDAO.updateCurrentBid(sessionId, amount, bidder.getId(), bidder.getFullName());

        // Cơ chế anti-sniping: gia hạn phiên nếu bid vào cuối
        if (session.getAntiSnipingSeconds() > 0) {
          LocalDateTime snipingWindow = session.getActualEndTime()
              .minusSeconds(session.getAntiSnipingSeconds());
          if (!bid.getTimestamp().isBefore(snipingWindow)) {
            LocalDateTime newEndTime = session.getActualEndTime()
                .plusSeconds(session.getAntiSnipingSeconds());
            auctionSessionDAO.updateActualEndTime(sessionId, newEndTime);
            System.out.printf("[AuctionBidService] Anti-sniping! Gia hạn đến: %s%n", newEndTime);
          }
        }

        // Ghi nhận bidder đã tham gia phiên
        if (!bidder.getParticipatedAuctions().contains(session)) {
          bidder.getParticipatedAuctions().add(session);
        }
        userDAO.updateBidderDetails(bidder);

        System.out.printf("[AuctionBidService] %s đặt %.0f VND cho phiên %s%n",
            bidder.getFullName(), amount, sessionId.substring(0, 8));

        // Bước 5d: Broadcast bid mới tới tất cả client đang xem phiên
        Dto.NewBidEvent event = new Dto.NewBidEvent(
            sessionId, bid.getId(), bidder.getId(), bidder.getFullName(),
            amount, LocalDateTime.now().toString(), bidType.name());
        Response response = Response.success(ActionType.NEW_BID_BROADCAST, event);
        AuctionBroadcaster.getInstance().broadcastToSession(sessionId, response);

        // Gửi thông báo chuông cho Seller và Bidder đã đăng ký
        NotificationService.getInstance().notifyNewBidPlaced(session, bid);

      } catch (SQLException e) {
        throw new RuntimeException("Lỗi database khi đặt giá: " + e.getMessage(), e);
      }

      return bid;
    }
  }
}
