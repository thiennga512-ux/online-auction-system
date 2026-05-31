package com.auction.server.dao;

import com.auction.service.auction.Bid;
import com.auction.server.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * Class BidDAO — Data Access Object cho Bid
 * ============================================================
 *
 * 🎓 GIẢI THÍCH:
 * BidDAO quản lý lịch sử đặt giá — đây là bảng được ghi nhiều nhất
 * trong toàn hệ thống (mỗi lượt đặt giá = 1 row mới).
 *
 * Đặc điểm quan trọng:
 * - Bid là IMMUTABLE (bất biến), nên KHÔNG có update/delete
 * - Chỉ có INSERT (thêm mới) và SELECT (đọc)
 * - Query phổ biến: lấy tất cả bid theo auctionSessionId
 * - Query hiệu năng cao nhờ INDEX trên auction_session_id
 * ============================================================
 */
public class BidDAO {

  private Connection getConnection() throws SQLException {
    return DatabaseManager.getInstance().getConnection();
  }

  // -------------------------------------------------------
  // CREATE
  // -------------------------------------------------------

  /**
   * Lưu một Bid mới vào database.
   * Bid là immutable → chỉ INSERT, không UPDATE.
   *
   * @param bid lượt đặt giá cần lưu
   */
  public void save(Bid bid) throws SQLException {
    String sql = """
        INSERT INTO bids
            (id, auction_session_id, bidder_id, bidder_name,
             amount, max_auto_bid, timestamp, bid_type)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, bid.getId());
      ps.setString(2, bid.getAuctionSessionId());
      ps.setString(3, bid.getBidderId());
      ps.setString(4, bid.getBidderName());
      ps.setDouble(5, bid.getAmount());
      // max_auto_bid có thể null → dùng setObject thay vì setDouble
      if (bid.getMaxAutoBid() != null) {
        ps.setDouble(6, bid.getMaxAutoBid());
      } else {
        ps.setNull(6, java.sql.Types.REAL);
      }
      ps.setString(7, bid.getTimestamp().toString());
      ps.setString(8, bid.getBidType().name());
      ps.executeUpdate();
    }
  }

  // -------------------------------------------------------
  // READ
  // -------------------------------------------------------

  /**
   * Lấy tất cả Bid của một phiên đấu giá, sắp xếp theo thời gian.
   * Dùng để tải lịch sử bid khi client mở màn hình đấu giá.
   *
   * @param auctionSessionId ID phiên đấu giá
   * @return danh sách Bid theo thứ tự thời gian tăng dần
   */
  public List<Bid> findBySessionId(String auctionSessionId) throws SQLException {
    List<Bid> bids = new ArrayList<>();
    String sql = """
        SELECT * FROM bids
        WHERE auction_session_id = ?
        ORDER BY timestamp ASC
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, auctionSessionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          bids.add(mapRowToBid(rs));
        }
      }
    }
    return bids;
  }

  /**
   * Lấy bid cao nhất trong một phiên (winning bid hiện tại).
   *
   * @param auctionSessionId ID phiên
   * @return Bid cao nhất (trừ INITIAL), hoặc null nếu chưa có bid
   */
  public Bid findHighestBid(String auctionSessionId) throws SQLException {
    String sql = """
        SELECT * FROM bids
        WHERE auction_session_id = ? AND bid_type != 'INITIAL'
        ORDER BY amount DESC, timestamp ASC
        LIMIT 1
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, auctionSessionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return mapRowToBid(rs);
      }
    }
    return null;
  }

  /**
   * Lấy tất cả Bid của một Bidder (lịch sử đặt giá cá nhân).
   *
   * @param bidderId ID người đặt giá
   * @return danh sách Bid theo thứ tự thời gian giảm dần (mới nhất trước)
   */
  public List<Bid> findByBidderId(String bidderId) throws SQLException {
    List<Bid> bids = new ArrayList<>();
    String sql = """
        SELECT * FROM bids
        WHERE bidder_id = ?
        ORDER BY timestamp DESC
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, bidderId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          bids.add(mapRowToBid(rs));
        }
      }
    }
    return bids;
  }

  /**
   * Đếm số lượt bid trong một phiên (hiển thị trên UI).
   *
   * @param auctionSessionId ID phiên
   * @return số lượt bid (trừ bid INITIAL)
   */
  public int countBySessionId(String auctionSessionId) throws SQLException {
    String sql = """
        SELECT COUNT(*) FROM bids
        WHERE auction_session_id = ? AND bid_type != 'INITIAL'
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, auctionSessionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  // -------------------------------------------------------
  // PRIVATE HELPER
  // -------------------------------------------------------

  /** Map ResultSet row → Bid object */
  private Bid mapRowToBid(ResultSet rs) throws SQLException {
    // Đọc max_auto_bid — có thể NULL trong database
    double maxAutoBidRaw = rs.getDouble("max_auto_bid");
    Double maxAutoBid = rs.wasNull() ? null : maxAutoBidRaw;
    // rs.wasNull() → true nếu giá trị vừa đọc là NULL

    return new Bid(
        rs.getString("id"),
        rs.getString("auction_session_id"),
        rs.getString("bidder_id"),
        rs.getString("bidder_name"),
        rs.getDouble("amount"),
        maxAutoBid,
        LocalDateTime.parse(rs.getString("timestamp")),
        Bid.BidType.valueOf(rs.getString("bid_type")));
  }
}
