package com.auction.server.service;

import com.auction.enums.AuctionStatus;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * AuctionService — Facade Pattern
 * ============================================================
 *
 * SOLID — Single Responsibility Principle (SRP):
 * Class này KHÔNG chứa business logic trực tiếp.
 * Nó đóng vai trò là Facade — trung gian duy nhất mà các
 * component bên ngoài (RequestDispatcher, AuctionTimerManager,
 * AutoBidService, ServerMain) gọi vào.
 *
 * Mọi nghiệp vụ cụ thể đã được tách ra thành:
 *   - AuctionQueryService       → Truy vấn dữ liệu
 *   - AuctionCreationService    → Tạo phiên mới
 *   - AuctionModerationService  → Duyệt / Từ chối / Huỷ phiên
 *   - AuctionLifecycleService   → Bắt đầu / Kết thúc phiên
 *   - AuctionBidService         → Đặt giá
 *
 * Lợi ích:
 *   - Backward-compatible: không cần sửa các class gọi vào
 *   - Dễ kiểm thử từng service riêng lẻ
 *   - Mỗi service chỉ có một lý do để thay đổi
 * ============================================================
 */
public class AuctionService {

  private final AuctionQueryService queryService;
  private final AuctionCreationService creationService;
  private final AuctionModerationService moderationService;
  private final AuctionLifecycleService lifecycleService;
  private final AuctionBidService bidService;

  /**
   * Constructor Injection — khởi tạo toàn bộ sub-services với các DAO được chia sẻ.
   */
  public AuctionService(AuctionSessionDAO auctionSessionDAO, BidDAO bidDAO,
      UserDAO userDAO, ItemService itemService) {

    this.queryService = new AuctionQueryService(auctionSessionDAO, bidDAO);
    this.creationService = new AuctionCreationService(auctionSessionDAO, itemService);
    this.moderationService = new AuctionModerationService(auctionSessionDAO, itemService, queryService);
    this.lifecycleService = new AuctionLifecycleService(auctionSessionDAO, bidDAO, userDAO, itemService, queryService);
    this.bidService = new AuctionBidService(auctionSessionDAO, bidDAO, userDAO, queryService);
  }

  // -------------------------------------------------------
  // TẠO PHIÊN — delegate → AuctionCreationService
  // -------------------------------------------------------

  public AuctionSession createAuction(Seller seller, String itemId,
      LocalDateTime startTime, LocalDateTime endTime, int antiSnipingSeconds) {
    return creationService.createAuction(seller, itemId, startTime, endTime, antiSnipingSeconds);
  }

  // -------------------------------------------------------
  // KIỂM DUYỆT PHIÊN — delegate → AuctionModerationService
  // -------------------------------------------------------

  public void approveAuction(String adminId, String sessionId) {
    moderationService.approveAuction(adminId, sessionId);
  }

  public void rejectAuction(String adminId, String sessionId, String reason) {
    moderationService.rejectAuction(adminId, sessionId, reason);
  }

  public void cancelAuction(User requester, String sessionId) {
    moderationService.cancelAuction(requester, sessionId);
  }

  // -------------------------------------------------------
  // VÒNG ĐỜI PHIÊN — delegate → AuctionLifecycleService
  // -------------------------------------------------------

  public void startAuction(String sessionId) {
    lifecycleService.startAuction(sessionId);
  }

  public void finishAuction(String sessionId) {
    lifecycleService.finishAuction(sessionId);
  }

  // -------------------------------------------------------
  // ĐẶT GIÁ — delegate → AuctionBidService
  // -------------------------------------------------------

  public Bid placeBid(Bidder bidder, String sessionId, double amount, Bid.BidType bidType) {
    return bidService.placeBid(bidder, sessionId, amount, bidType);
  }

  // -------------------------------------------------------
  // TRUY VẤN — delegate → AuctionQueryService
  // -------------------------------------------------------

  public List<AuctionSession> getActiveAuctions() {
    return queryService.getActiveAuctions();
  }

  public List<AuctionSession> getFinishedAuctions() {
    return queryService.getFinishedAuctions();
  }

  public List<AuctionSession> getAuctionsByStatus(AuctionStatus status) {
    return queryService.getAuctionsByStatus(status);
  }

  public List<AuctionSession> getAuctionsBySeller(String sellerId) {
    return queryService.getAuctionsBySeller(sellerId);
  }

  public List<Bid> getBidsBySession(String sessionId) {
    return queryService.getBidsBySession(sessionId);
  }

  public AuctionSession getSessionOrThrow(String sessionId) {
    return queryService.getSessionOrThrow(sessionId);
  }

  public Optional<AuctionSession> getSessionById(String sessionId) {
    return queryService.getSessionById(sessionId);
  }
}
