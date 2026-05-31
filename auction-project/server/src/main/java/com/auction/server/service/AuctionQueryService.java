package com.auction.server.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.auction.enums.AuctionStatus;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;

/**
 * ============================================================
 * AuctionQueryService — SRP: Truy vấn dữ liệu phiên đấu giá
 * ============================================================
 *
 * Chịu trách nhiệm DUY NHẤT: đọc/lấy dữ liệu phiên đấu giá
 * và lịch sử bid từ database. Không có nghiệp vụ thay đổi state.
 * ============================================================
 */
public class AuctionQueryService {

  private final AuctionSessionDAO auctionSessionDAO;
  private final BidDAO bidDAO;

  public AuctionQueryService(AuctionSessionDAO auctionSessionDAO, BidDAO bidDAO) {
    this.auctionSessionDAO = auctionSessionDAO;
    this.bidDAO = bidDAO;
  }

  /** Lấy tất cả phiên đang OPEN hoặc RUNNING */
  public List<AuctionSession> getActiveAuctions() {
    try {
      return auctionSessionDAO.findActiveAuctions();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /** Lấy tất cả phiên đã kết thúc */
  public List<AuctionSession> getFinishedAuctions() {
    try {
      return auctionSessionDAO.findFinishedAuctions();
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

  /** Lấy lịch sử bid của một phiên đấu giá */
  public List<Bid> getBidsBySession(String sessionId) {
    getSessionOrThrow(sessionId); // Đảm bảo session tồn tại
    try {
      return bidDAO.findBySessionId(sessionId);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /** Lấy AuctionSession theo ID, ném lỗi nếu không tìm thấy */
  public AuctionSession getSessionOrThrow(String sessionId) {
    try {
      return auctionSessionDAO.findById(sessionId)
          .orElseThrow(() -> new IllegalArgumentException(
              "Không tìm thấy phiên đấu giá: " + sessionId));
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  /** Public accessor để các service khác lấy thông tin phiên */
  public Optional<AuctionSession> getSessionById(String sessionId) {
    try {
      return auctionSessionDAO.findById(sessionId);
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }
}
