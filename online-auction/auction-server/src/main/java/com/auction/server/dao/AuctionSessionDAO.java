package com.auction.server.dao;

import com.auction.common.model.auction.AuctionSession;
import com.auction.common.model.auction.AuctionStatus;
import com.auction.common.model.item.Item;
import com.auction.server.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



/**
 * ============================================================
 * Class AuctionSessionDAO — Data Access Object cho AuctionSession
 * ============================================================
 *
 * 🎓 GIẢI THÍCH:
 * AuctionSessionDAO quản lý bảng auction_sessions.
 * Đây là bảng trung tâm — kết nối Items với Bids.
 *
 * Những query đặc biệt:
 *   - findByStatus(RUNNING): lấy tất cả phiên đang chạy
 *     → Dùng trong Timer để kiểm tra auto-finish khi hết giờ
 *   - updateCurrentPrice: cập nhật giá real-time khi có bid mới
 *     → Được gọi rất thường xuyên → cần index trên status
 *
 * Lưu ý: AuctionSession cần Item object → phụ thuộc vào ItemDAO.
 * ============================================================
 */
public class AuctionSessionDAO {

  private final ItemDAO itemDAO;
  private final BidDAO bidDAO;

  /**
   * Constructor Dependency Injection — truyền ItemDAO và BidDAO vào.
   * Tránh tạo instance mới bên trong DAO (hard coupling).
   */
  public AuctionSessionDAO(ItemDAO itemDAO, BidDAO bidDAO) {
    this.itemDAO = itemDAO;
    this.bidDAO = bidDAO;
  }

  // Raw data holder để lưu kết quả từ ResultSet trước khi gọi các DAO khác, đảm bảo an toàn cho Connection
  private record SessionRow(
      String id, String itemId, String sellerId, String sellerName,
      double currentPrice, String currentWinnerId, String currentWinnerName,
      String statusStr, String startTime, String endTime, String actualEndTime,
      String createdAt, int antiSnipingSeconds, String approvedByAdminId, String adminNote
  ) {}

  private Connection getConnection() {
    return DatabaseManager.getInstance().getConnection();
  }

  // -------------------------------------------------------
  // CREATE
  // -------------------------------------------------------

  /**
   * Lưu phiên đấu giá mới vào database.
   *
   * @param session phiên đấu giá cần lưu
   */
  public void save(AuctionSession session) throws SQLException {
    String sql = """
        INSERT INTO auction_sessions
            (id, item_id, seller_id, seller_name, current_price,
             current_winner_id, current_winner_name, status,
             start_time, end_time, actual_end_time, created_at,
             anti_sniping_seconds, approved_by_admin_id, admin_note)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, session.getId());
      ps.setString(2, session.getItem().getId());
      ps.setString(3, session.getSellerId());
      ps.setString(4, session.getSellerName());
      ps.setDouble(5, session.getCurrentPrice());
      ps.setString(6, session.getCurrentWinnerId());
      ps.setString(7, session.getCurrentWinnerName());
      ps.setString(8, session.getStatus().name());
      ps.setString(9, session.getStartTime().toString());
      ps.setString(10, session.getEndTime().toString());
      ps.setString(11, session.getActualEndTime().toString());
      ps.setString(12, session.getCreatedAt().toString());
      ps.setInt(13, session.getAntiSnipingSeconds());
      ps.setString(14, session.getApprovedByAdminId());
      ps.setString(15, session.getAdminNote());
      ps.executeUpdate();
    }
  }

  // -------------------------------------------------------
  // READ
  // -------------------------------------------------------

  /**
   * Tìm phiên đấu giá theo ID, bao gồm cả lịch sử bid.
   *
   * @param id ID phiên
   * @return Optional<AuctionSession>
   */
  public Optional<AuctionSession> findById(String id) throws SQLException {
    String sql = "SELECT * FROM auction_sessions WHERE id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          AuctionSession session = mapRowToSession(rs);
          // Load lịch sử bid vào session
          session.loadBids(bidDAO.findBySessionId(id));
          return Optional.of(session);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Lấy tất cả phiên theo trạng thái.
   * Query này được gọi bởi AuctionTimer để tìm phiên cần start/finish.
   *
   * @param status trạng thái cần lọc
   * @return danh sách AuctionSession
   */
  public List<AuctionSession> findByStatus(AuctionStatus status) throws SQLException {
    // Bước 1: Thu thập raw data (đóng ResultSet trước)
    List<SessionRow> rows = new ArrayList<>();
    String sql = "SELECT * FROM auction_sessions WHERE status = ? ORDER BY start_time ASC";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, status.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rows.add(extractRow(rs));
        }
      }
    }
    // Bước 2: Map sang AuctionSession (ResultSet đã đóng, an toàn gọi DAO con)
    List<AuctionSession> sessions = new ArrayList<>();
    for (SessionRow row : rows) {
      sessions.add(buildSession(row));
    }
    return sessions;
  }

  /**
   * Lấy tất cả phiên của một Seller (trang quản lý của Seller).
   *
   * @param sellerId ID người bán
   * @return danh sách phiên theo thứ tự mới nhất trước
   */
  public List<AuctionSession> findBySellerId(String sellerId) throws SQLException {
    List<SessionRow> rows = new ArrayList<>();
    String sql = """
        SELECT * FROM auction_sessions
        WHERE seller_id = ?
        ORDER BY created_at DESC
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rows.add(extractRow(rs));
        }
      }
    }
    List<AuctionSession> sessions = new ArrayList<>();
    for (SessionRow row : rows) {
      sessions.add(buildSession(row));
    }
    return sessions;
  }

  /**
   * Lấy tất cả phiên đã kết thúc hoặc bị hủy (Kết quả đấu giá).
   * Bao gồm FINISHED, CANCELLED — sắp xếp theo thời gian kết thúc giảm dần.
   *
   * @return danh sách phiên đã kết thúc hoặc bị hủy
   */
  public List<AuctionSession> findFinishedOrCancelledAuctions() throws SQLException {
    List<SessionRow> rows = new ArrayList<>();
    String sql = """
        SELECT * FROM auction_sessions
        WHERE status IN ('FINISHED', 'CANCELLED')
        ORDER BY actual_end_time DESC
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rows.add(extractRow(rs));
        }
      }
    }
    List<AuctionSession> sessions = new ArrayList<>();
    for (SessionRow row : rows) {
      sessions.add(buildSession(row));
    }
    return sessions;
  }

  /**
   * Lấy tất cả phiên đang OPEN hoặc RUNNING (hiển thị cho Bidder duyệt).
   *
   * @return danh sách phiên đang hoạt động
   */
  public List<AuctionSession> findActiveAuctions() throws SQLException {
    List<SessionRow> rows = new ArrayList<>();
    String sql = """
        SELECT * FROM auction_sessions
        WHERE status IN ('OPEN', 'RUNNING')
        ORDER BY end_time ASC
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rows.add(extractRow(rs));
        }
      }
    }
    List<AuctionSession> sessions = new ArrayList<>();
    for (SessionRow row : rows) {
      sessions.add(buildSession(row));
    }
    return sessions;
  }

  // -------------------------------------------------------
  // UPDATE
  // -------------------------------------------------------

  /**
   * Cập nhật trạng thái phiên đấu giá.
   * Gọi khi Admin duyệt (PENDING→OPEN), Timer start (OPEN→RUNNING),
   * Timer kết thúc (RUNNING→FINISHED).
   *
   * @param sessionId ID phiên
   * @param newStatus trạng thái mới
   */
  public void updateStatus(String sessionId, AuctionStatus newStatus) throws SQLException {
    String sql = "UPDATE auction_sessions SET status = ? WHERE id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, newStatus.name());
      ps.setString(2, sessionId);
      ps.executeUpdate();
    }
  }

  /**
   * Cập nhật trạng thái phiên đấu giá kèm admin_note và actual_end_time.
   * Gọi khi Admin từ chối (PENDING→CANCELLED) hoặc Admin/Seller huỷ phiên (RUNNING→CANCELLED).
   *
   * @param sessionId    ID phiên
   * @param newStatus    trạng thái mới (thường là CANCELLED)
   * @param adminNote    lý do từ chối/huỷ (có thể null)
   * @param actualEndTime thời điểm kết thúc thực tế (thường là now)
   */
  public void updateStatusWithNote(String sessionId, AuctionStatus newStatus,
      String adminNote, LocalDateTime actualEndTime) throws SQLException {
    String sql = """
        UPDATE auction_sessions
        SET status = ?, admin_note = ?, actual_end_time = ?
        WHERE id = ?
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, newStatus.name());
      ps.setString(2, adminNote);
      ps.setString(3, actualEndTime != null ? actualEndTime.toString() : null);
      ps.setString(4, sessionId);
      ps.executeUpdate();
    }
  }

  /**
   * Cập nhật giá hiện tại và người thắng khi có bid mới.
   * Method này được gọi RẤT THƯỜNG XUYÊN trong Giai đoạn 3.
   *
   * @param sessionId   ID phiên
   * @param newPrice    giá bid mới
   * @param winnerId    ID người đang dẫn đầu
   * @param winnerName  tên người đang dẫn đầu
   */
  public void updateCurrentBid(String sessionId, double newPrice,
      String winnerId, String winnerName) throws SQLException {
    String sql = """
        UPDATE auction_sessions
        SET current_price = ?, current_winner_id = ?, current_winner_name = ?
        WHERE id = ?
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setDouble(1, newPrice);
      ps.setString(2, winnerId);
      ps.setString(3, winnerName);
      ps.setString(4, sessionId);
      ps.executeUpdate();
    }
  }

  /**
   * Cập nhật actual_end_time khi anti-sniping gia hạn thêm thời gian.
   *
   * @param sessionId     ID phiên
   * @param newActualEnd  thời gian kết thúc mới (đã gia hạn)
   */
  public void updateActualEndTime(String sessionId, LocalDateTime newActualEnd) throws SQLException {
    String sql = "UPDATE auction_sessions SET actual_end_time = ? WHERE id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, newActualEnd.toString());
      ps.setString(2, sessionId);
      ps.executeUpdate();
    }
  }

  /**
   * Admin duyệt phiên: cập nhật status và approved_by.
   *
   * @param sessionId   ID phiên được duyệt
   * @param adminId     ID admin duyệt
   */
  public void approve(String sessionId, String adminId) throws SQLException {
    String sql = """
        UPDATE auction_sessions
        SET status = 'OPEN', approved_by_admin_id = ?
        WHERE id = ? AND status = 'PENDING'
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, adminId);
      ps.setString(2, sessionId);
      ps.executeUpdate();
    }
  }

  // -------------------------------------------------------
  // PRIVATE HELPER
  // -------------------------------------------------------

  /**
   * Đọc toàn bộ dữ liệu thô từ ResultSet vào record tạm.
   * QUAN TRỌNG: Phải đọc xong rồi mới gọi DAO con
   * để tránh conflict tài nguyên trên cùng một Database Connection.
   */
  private SessionRow extractRow(ResultSet rs) throws SQLException {
    return new SessionRow(
        rs.getString("id"),
        rs.getString("item_id"),
        rs.getString("seller_id"),
        rs.getString("seller_name"),
        rs.getDouble("current_price"),
        rs.getString("current_winner_id"),
        rs.getString("current_winner_name"),
        rs.getString("status"),
        rs.getString("start_time"),
        rs.getString("end_time"),
        rs.getString("actual_end_time"),
        rs.getString("created_at"),
        rs.getInt("anti_sniping_seconds"),
        rs.getString("approved_by_admin_id"),
        rs.getString("admin_note")
    );
  }

  /**
   * Chuyển SessionRow (đã đóng ResultSet) sang AuctionSession đầy đủ.
   * Lúc này an toàn để gọi itemDAO (mở ResultSet mới).
   */
  private AuctionSession buildSession(SessionRow row) throws SQLException {
    Item item = itemDAO.findById(row.itemId())
        .orElseThrow(() -> new SQLException("Item không tìm thấy: " + row.itemId()));

    String actualEndTimeStr = row.actualEndTime();
    LocalDateTime actualEndTime = (actualEndTimeStr != null && !actualEndTimeStr.isBlank())
        ? LocalDateTime.parse(actualEndTimeStr)
        : LocalDateTime.parse(row.endTime());

    return new AuctionSession(
        row.id(),
        item,
        row.sellerId(),
        row.sellerName(),
        row.currentPrice(),
        row.currentWinnerId(),
        row.currentWinnerName(),
        AuctionStatus.fromString(row.statusStr()),
        LocalDateTime.parse(row.startTime()),
        LocalDateTime.parse(row.endTime()),
        actualEndTime,
        LocalDateTime.parse(row.createdAt()),
        row.antiSnipingSeconds(),
        row.approvedByAdminId(),
        row.adminNote()
    );
  }

  // Giữ lại mapRowToSession cho findById (chỉ 1 row, không bị conflict)
  private AuctionSession mapRowToSession(ResultSet rs) throws SQLException {
    return buildSession(extractRow(rs));
  }
}
