package com.auction.server.service;

import java.sql.SQLException;

import com.auction.enums.AuctionStatus;
import com.auction.model.Admin;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.service.auction.AuctionSession;

/**
 * ============================================================
 * AuctionModerationService — SRP: Kiểm duyệt phiên đấu giá
 * ============================================================
 *
 * Chịu trách nhiệm DUY NHẤT: các thao tác quản trị/kiểm duyệt
 * phiên đấu giá do Admin hoặc Seller thực hiện.
 *   - Admin duyệt / từ chối phiên
 *   - Admin hoặc Seller chủ sở hữu huỷ phiên
 * ============================================================
 */
public class AuctionModerationService {

  private final AuctionSessionDAO auctionSessionDAO;
  private final ItemService itemService;
  private final AuctionQueryService queryService;

  public AuctionModerationService(AuctionSessionDAO auctionSessionDAO,
      ItemService itemService, AuctionQueryService queryService) {
    this.auctionSessionDAO = auctionSessionDAO;
    this.itemService = itemService;
    this.queryService = queryService;
  }

  /**
   * Admin duyệt phiên đấu giá: PENDING → OPEN.
   *
   * @param adminId   ID Admin duyệt
   * @param sessionId ID phiên cần duyệt
   */
  public void approveAuction(String adminId, String sessionId) {
    AuctionSession session = queryService.getSessionOrThrow(sessionId);

    if (session.getStatus() != AuctionStatus.PENDING) {
      throw new IllegalStateException(
          "Chỉ phiên PENDING mới cần duyệt. Hiện tại: " + session.getStatus());
    }

    try {
      auctionSessionDAO.approve(sessionId, adminId);
      System.out.printf("[AuctionModerationService] Admin %s đã duyệt phiên %s%n",
          adminId.substring(0, 8), sessionId.substring(0, 8));
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /**
   * Admin từ chối phiên đấu giá: PENDING → CANCELLED.
   *
   * @param adminId   ID Admin từ chối
   * @param sessionId ID phiên
   * @param reason    Lý do từ chối
   */
  public void rejectAuction(String adminId, String sessionId, String reason) {
    AuctionSession session = queryService.getSessionOrThrow(sessionId);

    if (session.getStatus() != AuctionStatus.PENDING) {
      throw new IllegalStateException("Chỉ phiên PENDING mới có thể từ chối");
    }

    try {
      auctionSessionDAO.updateStatus(sessionId, AuctionStatus.CANCELLED);
      itemService.markItemAsAvailable(session.getItem().getId());
      System.out.printf("[AuctionModerationService] Admin %s từ chối phiên %s. Lý do: %s%n",
          adminId.substring(0, 8), sessionId.substring(0, 8), reason);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /**
   * Admin hoặc Seller chủ sở hữu huỷ phiên đấu giá.
   *
   * @param requester Người thực hiện huỷ (Admin hoặc Seller)
   * @param sessionId ID phiên cần huỷ
   */
  public void cancelAuction(User requester, String sessionId) {
    AuctionSession session = queryService.getSessionOrThrow(sessionId);

    if (session.getStatus().isTerminal()) {
      throw new IllegalStateException("Phiên đã kết thúc hoặc đã bị huỷ");
    }
    boolean isAdmin = requester instanceof Admin;
    boolean isOwner = requester instanceof Seller
        && session.getSellerId().equals(requester.getId());

    if (!isAdmin && !isOwner) {
      throw new IllegalStateException("Bạn không có quyền huỷ phiên đấu giá này");
    }

    try {
      auctionSessionDAO.updateStatus(sessionId, AuctionStatus.CANCELLED);
      itemService.markItemAsAvailable(session.getItem().getId());
      System.out.printf("[AuctionModerationService] User %s đã huỷ phiên %s%n",
          requester.getFullName(), sessionId.substring(0, 8));
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }
}
