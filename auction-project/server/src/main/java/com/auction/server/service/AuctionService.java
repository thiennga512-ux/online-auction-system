package com.auction.server.service;

import java.util.concurrent.ConcurrentHashMap;

import com.auction.enums.ActionType;
import com.auction.enums.AuctionStatus;
import com.auction.model.Seller;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.dto.Dto;
import com.auction.network.Response;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.observer.AuctionBroadcaster;
import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.sql.SQLException;
import java.util.Optional;

/**
 * ============================================================
 * Class AuctionService — Business Logic cho Phiên Đấu Giá
 * ============================================================
 *
 * 🎓 GIẢI THÍCH — Đây là Service phức tạp nhất:
 *
 * AuctionService chứa toàn bộ logic nghiệp vụ của phiên đấu giá:
 * 1. Tạo phiên mới (Seller)
 * 2. Duyệt phiên (Admin)
 * 3. Tự động start/finish phiên (Timer — Giai đoạn 3)
 * 4. Xử lý bid — ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT
 * 5. Tính phí hoa hồng, chuyển tiền Seller
 *
 * ⚠️ LƯU Ý CONCURRENCY (Giai đoạn 3):
 * Method placeBid() sẽ cần xử lý đồng thời (synchronized).
 * Giai đoạn 2 này chưa làm, sẽ nâng cấp ở Giai đoạn 3.
 * ============================================================
 */
public class AuctionService {

  private final AuctionSessionDAO auctionSessionDAO;
  private final BidDAO bidDAO;
  private final UserDAO userDAO;
  private final ItemService itemService;

  // Lắp cơ chế đồng bộ (Lock) theo từng phiên để tránh blocking chéo
  private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();
  // Lock theo item để tránh tạo trùng nhiều phiên cho cùng 1 sản phẩm
  private final Map<String, Object> itemLocks = new ConcurrentHashMap<>();

  /**
   * Constructor Injection — nhận các dependency từ ngoài.
   */
  public AuctionService(AuctionSessionDAO auctionSessionDAO, BidDAO bidDAO,
      UserDAO userDAO, ItemService itemService) {
    this.auctionSessionDAO = auctionSessionDAO;
    this.bidDAO = bidDAO;
    this.userDAO = userDAO;
    this.itemService = itemService;
  }

  // -------------------------------------------------------
  // TẠO PHIÊN ĐẤU GIÁ (Seller)
  // -------------------------------------------------------

  /**
   * Seller tạo một phiên đấu giá mới cho sản phẩm của mình.
   *
   * Validation:
   * - endTime phải sau startTime ít nhất 1 giờ
   * - startTime phải trong tương lai (>=5 phút từ bây giờ)
   * - Item phải thuộc Seller và còn available
   *
   * @param seller             seller tạo phiên
   * @param itemId             ID sản phẩm muốn đấu giá
   * @param startTime          thời gian bắt đầu nhận bid
   * @param endTime            thời gian kết thúc dự kiến
   * @param antiSnipingSeconds giây gia hạn anti-sniping (0 = tắt)
   * @return AuctionSession mới tạo (chờ Admin duyệt)
   */
  public AuctionSession createAuction(Seller seller, String itemId,
      LocalDateTime startTime, LocalDateTime endTime, int antiSnipingSeconds) {
    Object lock = itemLocks.computeIfAbsent(itemId, k -> new Object());
    synchronized (lock) {

      // Bước 1: Validate thời gian
      validateAuctionTime(startTime, endTime);

      // Bước 2: Lấy Item và kiểm tra quyền sở hữu
      Item item = itemService.findById(itemId)
          .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + itemId));

      if (!item.getSellerId().equals(seller.getId())) {
        throw new IllegalArgumentException("Bạn không có quyền đấu giá sản phẩm này");
      }
      if (!item.isAvailable()) {
        throw new IllegalArgumentException("Sản phẩm này đã được bán hoặc đang trong phiên đấu giá khác");
      }

      // Bước 3: Tạo AuctionSession mới (trạng thái PENDING)
      AuctionSession session = new AuctionSession(
          item, seller.getId(), seller.getFullName(),
          startTime, endTime, antiSnipingSeconds);

      // Bước 4: Lưu vào database
      try {
        auctionSessionDAO.save(session);
        System.out.printf("[AuctionService] Tạo phiên mới: %s | Sản phẩm: %s%n",
            session.getId().substring(0, 8), item.getName());
      } catch (SQLException e) {
        throw new RuntimeException("Lỗi khi tạo phiên đấu giá: " + e.getMessage(), e);
      }

      return session;
    }
  }

  /** Validate thời gian phiên đấu giá */
  private void validateAuctionTime(LocalDateTime startTime, LocalDateTime endTime) {
    LocalDateTime now = LocalDateTime.now();

    if (startTime.isBefore(now.plusMinutes(5))) {
      throw new IllegalArgumentException(
          "Thời gian bắt đầu phải ít nhất 5 phút từ bây giờ");
    }
    if (!endTime.isAfter(startTime.plusHours(1))) {
      throw new IllegalArgumentException(
          "Phiên đấu giá phải kéo dài ít nhất 1 giờ");
    }
    if (endTime.isAfter(startTime.plusDays(30))) {
      throw new IllegalArgumentException(
          "Phiên đấu giá không được kéo dài quá 30 ngày");
    }
  }

  // -------------------------------------------------------
  // DUYỆT PHIÊN (Admin)
  // -------------------------------------------------------

  /**
   * Admin duyệt phiên đấu giá (PENDING → OPEN).
   *
   * @param adminId   ID admin duyệt
   * @param sessionId ID phiên cần duyệt
   */
  public void approveAuction(String adminId, String sessionId) {
    AuctionSession session = getSessionOrThrow(sessionId);

    if (session.getStatus() != AuctionStatus.PENDING) {
      throw new IllegalStateException(
          "Chỉ phiên PENDING mới cần duyệt. Hiện tại: " + session.getStatus());
    }

    try {
      auctionSessionDAO.approve(sessionId, adminId);
      System.out.printf("[AuctionService] Admin %s đã duyệt phiên %s%n",
          adminId.substring(0, 8), sessionId.substring(0, 8));
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /**
   * Admin từ chối phiên đấu giá (PENDING → CANCELLED).
   *
   * @param adminId   ID admin từ chối
   * @param sessionId ID phiên
   * @param reason    lý do từ chối
   */
  public void rejectAuction(String adminId, String sessionId, String reason) {
    AuctionSession session = getSessionOrThrow(sessionId);

    if (session.getStatus() != AuctionStatus.PENDING) {
      throw new IllegalStateException("Chỉ phiên PENDING mới có thể từ chối");
    }

    try {
      auctionSessionDAO.updateStatus(sessionId, AuctionStatus.CANCELLED);
      System.out.printf("[AuctionService] Admin %s từ chối phiên %s. Lý do: %s%n",
          adminId.substring(0, 8), sessionId.substring(0, 8), reason);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /**
   * Huỷ phiên đấu giá (Admin hoặc Seller).
   *
   * @param requester User yêu cầu huỷ
   * @param sessionId ID phiên
   */
  public void cancelAuction(User requester, String sessionId) {
    AuctionSession session = getSessionOrThrow(sessionId);

    if (session.getStatus().isTerminal()) {
      throw new IllegalStateException("Phiên đã kết thúc hoặc đã bị huỷ");
    }

    // Chỉ Admin hoặc Seller của phiên mới được quyền huỷ
    boolean isAdmin = requester instanceof Admin;
    boolean isOwner = requester instanceof Seller && session.getSellerId().equals(requester.getId());

    if (!isAdmin && !isOwner) {
      throw new IllegalStateException("Bạn không có quyền huỷ phiên đấu giá này");
    }

    try {
      auctionSessionDAO.updateStatus(sessionId, AuctionStatus.CANCELLED);
      System.out.printf("[AuctionService] User %s đã huỷ phiên %s%n",
          requester.getFullName(), sessionId.substring(0, 8));

      // Update item status back to available
      // Not directly required but good practice if itemDAO has a way to mark
      // available.
      // Currently itemDAO only has markAsSold. It is available by default until sold.
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  // -------------------------------------------------------
  // TIMER — Start / Finish tự động (sẽ nâng cấp Giai đoạn 3)
  // -------------------------------------------------------

  /**
   * Bắt đầu phiên đấu giá (OPEN → RUNNING).
   * Giai đoạn 3: sẽ được gọi bởi ScheduledExecutorService timer.
   *
   * @param sessionId ID phiên cần bắt đầu
   */
  public void startAuction(String sessionId) {
    AuctionSession session = getSessionOrThrow(sessionId);

    if (session.getStatus() != AuctionStatus.OPEN) {
      throw new IllegalStateException("Phiên không ở trạng thái OPEN: " + session.getStatus());
    }

    try {
      auctionSessionDAO.updateStatus(sessionId, AuctionStatus.RUNNING);
      // Tạo bid giá khởi điểm (INITIAL bid)
      Bid initialBid = Bid.createInitial(sessionId, session.getItem().getStartingPrice());
      bidDAO.save(initialBid);

      System.out.printf("[AuctionService] Phiên %s đã BẮT ĐẦU! Giá khởi điểm: %.0f VND%n",
          sessionId.substring(0, 8), session.getItem().getStartingPrice());

      // Broadcast sự kiện BẮT ĐẦU
      Dto.AuctionStartedEvent event = new Dto.AuctionStartedEvent(sessionId, session.getItem().getStartingPrice());
      Response response = Response.success(ActionType.AUCTION_STARTED_BROADCAST, "Phiên đấu giá đã bắt đầu!", event);
      AuctionBroadcaster.getInstance().broadcastToSession(sessionId, response);

      // Gửi thông báo email cho Seller và Bidders đăng ký
      com.auction.server.service.NotificationService.getInstance().notifyAuctionStarted(session);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /**
   * Kết thúc phiên đấu giá (RUNNING → FINISHED).
   * Giai đoạn 3: gọi bởi Timer khi hết giờ.
   * Giai đoạn 4: notify kết quả cho tất cả Client qua Socket.
   *
   * @param sessionId ID phiên cần kết thúc
   */
  public void finishAuction(String sessionId) {
    AuctionSession session = getSessionOrThrow(sessionId);

    if (session.getStatus() != AuctionStatus.RUNNING) {
      throw new IllegalStateException("Phiên không đang RUNNING: " + session.getStatus());
    }

    try {
      // Cập nhật trạng thái
      auctionSessionDAO.updateStatus(sessionId, AuctionStatus.FINISHED);

      // Nếu có người thắng → đánh dấu sản phẩm đã bán + chuyển tiền Seller
      if (session.getCurrentWinnerId() != null) {
        handleAuctionWin(session);
      } else {
        System.out.printf("[AuctionService] Phiên %s kết thúc — KHÔNG có người thắng%n",
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

      // Gửi thông báo email cho Seller và Bidders đăng ký (nếu chưa được gửi)
      com.auction.server.service.NotificationService.getInstance().notifyAuctionEnded(session);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database khi kết thúc phiên: " + e.getMessage(), e);
    }
  }

  private double calculateCommissionFee(AuctionSession session) {
    return session.getCurrentPrice() * 0.05; // 5% mặc định
  }

  /** Xử lý khi phiên đấu giá có người thắng */
  private void handleAuctionWin(AuctionSession session) throws SQLException {
    double finalPrice = session.getCurrentPrice();
    double commission = calculateCommissionFee(session);
    double sellerReceives = finalPrice - commission;

    // Đánh dấu sản phẩm đã bán
    itemService.markItemAsSold(session.getItem().getId());

    // Chuyển tiền cho Seller (trừ hoa hồng)
    Optional<User> sellerOpt = userDAO.findById(session.getSellerId());
    if (sellerOpt.isPresent() && sellerOpt.get() instanceof Seller seller) {
      seller.setBalance(seller.getBalance() + sellerReceives);
      userDAO.updateSellerDetails(seller);

      // Gửi email cho Seller
      String sellerSubject = "Phiên đấu giá kết thúc: " + session.getItem().getName() + " đã được bán!";
      String sellerBody = "Chúc mừng " + seller.getFullName() + ",\n\n"
          + "Sản phẩm '" + session.getItem().getName() + "' của bạn đã được bán thành công.\n"
          + "Giá bán: " + String.format("%,.0f", finalPrice) + " VND.\n"
          + "Phí hoa hồng: " + String.format("%,.0f", commission) + " VND.\n"
          + "Số tiền thực nhận: " + String.format("%,.0f", sellerReceives)
          + " VND đã được cộng vào số dư tài khoản.\n\n"
          + "Trân trọng,\nBan quản trị hệ thống";
      EmailService.sendEmailAsync(seller.getEmail(), sellerSubject, sellerBody);

      com.auction.server.network.ClientHandler sellerHandler = com.auction.server.network.UserConnectionManager
          .getInstance().getHandler(seller.getId());
      if (sellerHandler != null) {
        Dto.DepositResultResponse result = new Dto.DepositResultResponse(seller.getBalance());
        sellerHandler
            .sendResponse(Response.success(ActionType.DEPOSIT_BALANCE, "Thanh toán thành công từ phiên đấu giá", result));
      }
    }

    // Gửi email cho Người thắng (Bidder)
    Optional<User> winnerOpt = userDAO.findById(session.getCurrentWinnerId());
    if (winnerOpt.isPresent() && winnerOpt.get() instanceof Bidder winner) {
      // TRỪ TIỀN NGƯỜI THẮNG (Tiền này đã bị đóng băng trong placeBid)
      winner.setFrozenBalance(winner.getFrozenBalance() - finalPrice);
      userDAO.updateBidderDetails(winner);

      String winnerSubject = "Chúc mừng bạn đã thắng đấu giá: " + session.getItem().getName();
      String winnerBody = "Xin chào " + winner.getFullName() + ",\n\n"
          + "Chúc mừng bạn đã là người chiến thắng phiên đấu giá cho sản phẩm '" + session.getItem().getName() + "'.\n"
          + "Giá chiến thắng: " + String.format("%,.0f", finalPrice) + " VND.\n\n"
          + "Số tiền đã được trừ vào số dư ký quỹ của bạn.\n"
          + "Vui lòng kiểm tra Dashboard để cập nhật trạng thái nhận hàng.\n\n"
          + "Trân trọng,\nBan quản trị hệ thống";
      EmailService.sendEmailAsync(winner.getEmail(), winnerSubject, winnerBody);

      com.auction.server.network.ClientHandler winnerHandler = com.auction.server.network.UserConnectionManager
          .getInstance().getHandler(winner.getId());
      if (winnerHandler != null) {
        double bal = winner.getBalance();
        Dto.DepositResultResponse result = new Dto.DepositResultResponse(bal);
        winnerHandler
            .sendResponse(Response.success(ActionType.DEPOSIT_BALANCE, "Thanh toán thành công phiên đấu giá", result));
      }
    }

    System.out.printf(
        "[AuctionService] Phiên %s KẾT THÚC!%n"
            + "  🏆 Người thắng: %s%n"
            + "  💰 Giá thắng: %.0f VND%n"
            + "  📊 Hoa hồng (%s): %.0f VND%n"
            + "  💵 Seller nhận: %.0f VND%n",
        session.getId().substring(0, 8),
        session.getCurrentWinnerName(),
        finalPrice,
        session.getItem().getCategory().name(),
        commission,
        sellerReceives);
  }

  // -------------------------------------------------------
  // ĐẶT GIÁ (Bidder)
  // -------------------------------------------------------

  /**
   * Xử lý một lượt đặt giá thủ công từ Bidder.
   *
   * ⚠️ GIAI ĐOẠN 3: Method này sẽ thêm "synchronized" để tránh
   * race condition (Lost Update Problem) khi nhiều Bidder đặt đồng thời.
   *
   * Flow:
   * 1. Validate phiên đang RUNNING
   * 2. Validate giá hợp lệ (> currentPrice + minIncrement)
   * 3. Tạo Bid object
   * 4. Cập nhật currentPrice, currentWinner trong DB
   * 5. Lưu Bid vào DB
   * 6. (Giai đoạn 3) Broadcast update đến tất cả Client
   *
   * @param bidder    người đặt giá
   * @param sessionId ID phiên đấu giá
   * @param amount    số tiền muốn đặt
   * @return Bid vừa được tạo và lưu
   */
  public Bid placeBid(Bidder bidder, String sessionId, double amount) {
    // 🎓 SYNCHRONIZED — Thread-safety!
    // Tránh Race Condition: 2 người cùng ném giá vào 1 mili-giây, server có thể đọc
    // sai giá hiện tại.
    // Việc tính toán lock theo sessionId giúp các phiên khác (ID khác) không bị
    // chặn (Lock Striping).
    Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());

    synchronized (lock) {
      // Bước 1: Lấy phiên đấu giá TỪ DATABASE ĐỂ ĐẢM BẢO DATA MỚI NHẤT
      AuctionSession session = getSessionOrThrow(sessionId);

      // Bước 2: Kiểm tra phiên có đang RUNNING không
      if (!session.getStatus().isAcceptingBids()) {
        throw new IllegalStateException(
            "Phiên không nhận bid. Trạng thái: " + session.getStatus().getDisplayName());
      }

      // Bước 3: Kiểm tra giá và số dư
      Item item = session.getItem();
      double minRequired = session.getCurrentPrice() + item.getBidIncrement();
      if (amount < minRequired) {
        throw new IllegalArgumentException(
            String.format("Giá đặt %.0f VND không hợp lệ. Tối thiểu: %.0f VND",
                amount, minRequired));
      }

      if (bidder.getBalance() < amount) {
        throw new IllegalArgumentException(
            "Số dư ký quỹ không đủ để đặt giá (Cần: " + String.format("%,.0f", amount) + " VND)");
      }

      // Bước 4: Tạo Bid mới
      Bid bid = Bid.createManual(sessionId, bidder.getId(), bidder.getFullName(), amount);

      // Bước 5: Cập nhật DB và quản lý dòng tiền
      try {
        // --- QUẢN LÝ DÒNG TIỀN (Observer & State Management) ---

        // 1. Giải phóng tiền cho người bị vượt giá (nếu có)
        String oldWinnerId = session.getCurrentWinnerId();
        double oldPrice = session.getCurrentPrice();

        if (oldWinnerId != null) {
          Optional<User> oldWinnerOpt = userDAO.findById(oldWinnerId);
          if (oldWinnerOpt.isPresent() && oldWinnerOpt.get() instanceof Bidder oldWinner) {
            // Chỉ giải phóng nếu người cũ khác người mới
            oldWinner.setFrozenBalance(oldWinner.getFrozenBalance() - oldPrice);
            oldWinner.setBalance(oldWinner.getBalance() + oldPrice);
            userDAO.updateBidderDetails(oldWinner);

            // --- Cập nhật realtime cho người cũ (nếu là Bidder) ---
            com.auction.server.network.ClientHandler oldWinnerHandler = com.auction.server.network.UserConnectionManager.getInstance().getHandler(oldWinner.getId());
            if (oldWinnerHandler != null) {
                Dto.DepositResultResponse result = new Dto.DepositResultResponse(oldWinner.getBalance());
                oldWinnerHandler.sendResponse(Response.success(ActionType.DEPOSIT_BALANCE, "Đã hoàn trả số dư đóng băng", result));
            }
          }
        }

        // 2. Đóng băng tiền của người vừa đặt giá mới
        bidder.setBalance(bidder.getBalance() - amount);
        bidder.setFrozenBalance(bidder.getFrozenBalance() + amount);
        userDAO.updateBidderDetails(bidder);

        // --- Cập nhật realtime cho người đặt giá (nếu là Bidder) ---
        com.auction.server.network.ClientHandler bidderHandler = com.auction.server.network.UserConnectionManager.getInstance().getHandler(bidder.getId());
        if (bidderHandler != null) {
            Dto.DepositResultResponse result = new Dto.DepositResultResponse(bidder.getBalance());
            bidderHandler.sendResponse(Response.success(ActionType.DEPOSIT_BALANCE, "Đã đóng băng số dư", result));
        }

        // --- CẬP NHẬT TRẠNG THÁI PHIÊN ---

        // Lưu bid vào lịch sử
        bidDAO.save(bid);

        // Cập nhật currentPrice và winner trong auction_sessions table
        auctionSessionDAO.updateCurrentBid(sessionId, amount,
            bidder.getId(), bidder.getFullName());

        // Nếu bid trong X giây cuối → cập nhật actual_end_time (anti-sniping)
        if (session.getAntiSnipingSeconds() > 0) {
          LocalDateTime snipingWindow = session.getActualEndTime()
              .minusSeconds(session.getAntiSnipingSeconds());
          if (!bid.getTimestamp().isBefore(snipingWindow)) {
            LocalDateTime newEndTime = session.getActualEndTime()
                .plusSeconds(session.getAntiSnipingSeconds());
            auctionSessionDAO.updateActualEndTime(sessionId, newEndTime);
            System.out.printf("[AuctionService] ⏰ Anti-sniping! Gia hạn đến: %s%n", newEndTime);
          }
        }

        // Ghi nhận bidder đã tham gia phiên này
        if (!bidder.getParticipatedAuctions().contains(session)) {
            bidder.getParticipatedAuctions().add(session);
        }
        userDAO.updateBidderDetails(bidder);

        System.out.printf("[AuctionService] 💰 %s đặt %.0f VND cho phiên %s%n",
            bidder.getFullName(), amount, sessionId.substring(0, 8));

        // Bắn tín hiệu có bid mới đến tất cả những người đang xem phiên thông qua
        // Socket.
        Dto.NewBidEvent event = new Dto.NewBidEvent(
            sessionId, bid.getId(), bidder.getId(), bidder.getFullName(), amount, bid.getTimestamp().toString());
        Response response = Response.success(ActionType.NEW_BID_BROADCAST, event);
        AuctionBroadcaster.getInstance().broadcastToSession(sessionId, response);

        // Gửi email cho các bidder đăng ký nhận thông báo (trừ người vừa đặt giá)
        com.auction.server.service.NotificationService.getInstance().notifyNewBidPlaced(session, bid);

      } catch (SQLException e) {
        throw new RuntimeException("Lỗi database khi đặt giá: " + e.getMessage(), e);
      }

      return bid;
    }
  }

  // -------------------------------------------------------
  // QUERY — Lấy danh sách phiên
  // -------------------------------------------------------
  /** Lấy tất cả phiên đang OPEN hoặc RUNNING */
  public List<AuctionSession> getActiveAuctions() {
    try {
      return auctionSessionDAO.findActiveAuctions();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /** Lấy phiên theo trạng thái */
  public List<AuctionSession> getAuctionsByStatus(AuctionStatus status) {
    try {
      return auctionSessionDAO.findByStatus(status);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /** Lấy tất cả phiên của một Seller */
  public List<AuctionSession> getAuctionsBySeller(String sellerId) {
    try {
      return auctionSessionDAO.findBySellerId(sellerId);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /** Lấy lịch sử bid của một phiên đấu giá theo thời gian tăng dần */
  public List<Bid> getBidsBySession(String sessionId) {
    // Đảm bảo session tồn tại trước khi trả lịch sử
    getSessionOrThrow(sessionId);
    try {
      return bidDAO.findBySessionId(sessionId);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  // -------------------------------------------------------
  // PRIVATE HELPER
  // -------------------------------------------------------

  /** Lấy AuctionSession theo ID, ném lỗi nếu không tìm thấy */
  private AuctionSession getSessionOrThrow(String sessionId) {
    try {
      return auctionSessionDAO.findById(sessionId)
          .orElseThrow(() -> new IllegalArgumentException(
              "Không tìm thấy phiên đấu giá: " + sessionId));
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }
}
