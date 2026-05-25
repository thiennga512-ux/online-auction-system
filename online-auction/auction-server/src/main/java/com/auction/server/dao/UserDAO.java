package com.auction.server.dao;

import com.auction.common.factory.UserFactory;
import com.auction.common.model.user.Admin;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.User;
import com.auction.common.model.user.UserRole;
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
 * Class UserDAO — Data Access Object cho User
 * ============================================================
 *
 * 🎓 DAO PATTERN LÀ GÌ?
 *
 * DAO (Data Access Object) là lớp chuyên biệt chịu trách nhiệm
 * tất cả các thao tác đọc/ghi vào database cho một loại entity.
 *
 * Tại sao tách riêng DAO thay vì viết SQL trực tiếp trong Service?
 *   ❌ KHÔNG dùng DAO: SQL nằm rải rác khắp code → khó bảo trì
 *   ✅ CÓ DAO: SQL tập trung một chỗ → dễ debug, dễ thay đổi DB
 *
 * Kiến trúc:
 *   Controller → Service → DAO → Database
 *
 * UserDAO xử lý:
 *   - Bảng "users" (thông tin chung)
 *   - Bảng "admin_details", "seller_details", "bidder_details" (thông tin riêng)
 *
 * SQL dùng PreparedStatement (KHÔNG nối chuỗi trực tiếp) để tránh SQL Injection.
 * ============================================================
 */
public class UserDAO {

  /** Lấy connection qua Singleton DatabaseManager */
  private Connection getConnection() {
    return DatabaseManager.getInstance().getConnection();
  }

  // -------------------------------------------------------
  // CREATE — Lưu User mới vào database
  // -------------------------------------------------------

  /**
   * Lưu một User mới vào database.
   * Tự động phân nhánh theo role để insert bảng detail tương ứng.
   *
   * 🎓 PreparedStatement là gì?
   *   Thay vì: "INSERT INTO users VALUES('" + id + "', ...)" — NGUY HIỂM!
   *   Dùng:    "INSERT INTO users VALUES(?, ?, ...)"
   *   → Dấu ? = placeholder, giá trị được truyền riêng biệt
   *   → MySQL/JDBC tự escape → không bị SQL Injection
   *
   * @param user User cần lưu (Admin, Seller, hoặc Bidder)
   * @throws SQLException nếu lỗi database
   */
  public void save(User user) throws SQLException {
    // Bước 1: Insert vào bảng users (thông tin chung)
    String sql = """
        INSERT INTO users (id, full_name, username, email, password_hash,
                           phone_number, gender, date_of_birth, created_at, active, role)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, user.getId());
      ps.setString(2, user.getFullName());
      ps.setString(3, user.getUsername());
      ps.setString(4, user.getEmail());
      ps.setString(5, user.getPasswordHash());
      ps.setString(6, user.getPhoneNumber());
      ps.setString(7, user.getGender());
      ps.setString(8, user.getDateOfBirth());
      ps.setString(9, user.getCreatedAt().toString()); // LocalDateTime → String ISO
      ps.setBoolean(10, user.isActive());               // boolean tự động map với TINYINT(1) trong MySQL
      ps.setString(11, user.getRole().name());
      ps.executeUpdate();
    }

    // Bước 2: Insert vào bảng detail tương ứng
    switch (user.getRole()) {
      case ADMIN -> saveAdminDetails((Admin) user);
      case SELLER -> saveSellerDetails((Seller) user);
      case BIDDER -> saveBidderDetails((Bidder) user);
    }
  }

  /** Lưu thông tin chi tiết của Admin */
  private void saveAdminDetails(Admin admin) throws SQLException {
    String sql = "INSERT INTO admin_details (user_id, admin_notes) VALUES (?, ?)";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, admin.getId());
      ps.setString(2, admin.getAdminNotes() != null ? admin.getAdminNotes() : "");
      ps.executeUpdate();
    }
  }

  /** Lưu thông tin chi tiết của Seller */
  private void saveSellerDetails(Seller seller) throws SQLException {
    String sql = """
        INSERT INTO seller_details (user_id, shop_name, citizen_id, rating, rating_count, balance)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, seller.getId());
      ps.setString(2, seller.getShopName());
      ps.setString(3, seller.getCitizenId());
      ps.setDouble(4, seller.getRating());
      ps.setInt(5, seller.getRatingCount());
      ps.setDouble(6, seller.getBalance());
      ps.executeUpdate();
    }
  }

  /** Lưu thông tin chi tiết của Bidder */
  private void saveBidderDetails(Bidder bidder) throws SQLException {
    String sql = """
        INSERT INTO bidder_details (user_id, deposit_balance, frozen_balance, shipping_address, total_bids_placed)
        VALUES (?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, bidder.getId());
      ps.setDouble(2, bidder.getDepositBalance());
      ps.setDouble(3, bidder.getFrozenBalance());
      ps.setString(4, bidder.getShippingAddress());
      ps.setInt(5, bidder.getTotalBidsPlaced());
      ps.executeUpdate();
    }
  }

  // -------------------------------------------------------
  // READ — Đọc User từ database
  // -------------------------------------------------------

  /**
   * Tìm User theo ID.
   *
   * 🎓 Optional<T> là gì?
   *   Thay vì trả về null (gây NullPointerException), ta trả về Optional.
   *   Optional.of(user)    → có user
   *   Optional.empty()     → không tìm thấy
   *   Code gọi: userDAO.findById(id).ifPresent(u -> ...)
   *   → Buộc code gọi phải xử lý trường hợp không tìm thấy
   *
   * @param id ID cần tìm
   * @return Optional chứa User nếu tìm thấy, Optional.empty() nếu không
   */
  public Optional<User> findById(String id) throws SQLException {
    String sql = "SELECT * FROM users WHERE id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToUser(rs));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Tìm User theo email (dùng khi đăng nhập).
   * Email là UNIQUE trong bảng users → chỉ có 0 hoặc 1 kết quả.
   *
   * @param email email cần tìm
   * @return Optional<User>
   */
  public Optional<User> findByEmail(String email) throws SQLException {
    String sql = "SELECT * FROM users WHERE email = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToUser(rs));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Lấy tất cả User theo role.
   * Ví dụ: findAllByRole("SELLER") → tất cả Seller trong hệ thống.
   *
   * @param role vai trò cần lọc
   * @return danh sách User
   */
  public List<User> findAllByRole(UserRole role) throws SQLException {
    List<User> result = new ArrayList<>();
    String sql = "SELECT * FROM users WHERE role = ? AND active = 1";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, role.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRowToUser(rs));
        }
      }
    }
    return result;
  }

  /**
   * Lấy tất cả User (dành cho Admin quản lý).
   */
  public List<User> findAll() throws SQLException {
    List<User> result = new ArrayList<>();
    String sql = "SELECT * FROM users ORDER BY created_at DESC";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRowToUser(rs));
        }
      }
    }
    return result;
  }

  /**
   * Đếm tổng số user trong hệ thống.
   */
  public int count() throws SQLException {
    String sql = "SELECT COUNT(*) FROM users";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  /**
   * Kiểm tra email đã tồn tại chưa (trước khi đăng ký).
   *
   * @param email email cần kiểm tra
   * @return true nếu đã tồn tại
   */
  public boolean emailExists(String email) throws SQLException {
    String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    }
  }

  /**
   * Kiểm tra CCCD đã được dùng bởi Seller khác chưa.
   */
  public boolean citizenIdExists(String citizenId) throws SQLException {
    String sql = "SELECT COUNT(*) FROM seller_details WHERE citizen_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, citizenId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    }
  }

  // -------------------------------------------------------
  // UPDATE — Cập nhật thông tin User
  // -------------------------------------------------------

  /**
   * Cập nhật thông tin cơ bản của User.
   *
   * @param user User đã được chỉnh sửa
   */
  public void update(User user) throws SQLException {
    String sql = """
        UPDATE users
        SET full_name = ?, username = ?, email = ?, phone_number = ?, gender = ?, date_of_birth = ?, active = ?
        WHERE id = ?
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, user.getFullName());
      ps.setString(2, user.getUsername());
      ps.setString(3, user.getEmail());
      ps.setString(4, user.getPhoneNumber());
      ps.setString(5, user.getGender());
      ps.setString(6, user.getDateOfBirth());
      ps.setBoolean(7, user.isActive());
      ps.setString(8, user.getId());
      ps.executeUpdate();
    }

    // Cập nhật bảng detail tương ứng
    switch (user.getRole()) {
      case SELLER -> updateSellerDetails((Seller) user);
      case BIDDER -> updateBidderDetails((Bidder) user);
      case ADMIN -> { /* Admin ít thay đổi, bỏ qua */ }
    }
  }

  /** Cập nhật thông tin Seller (balance, rating...) */
  public void updateSellerDetails(Seller seller) throws SQLException {
    String sql = """
        UPDATE seller_details
        SET shop_name = ?, citizen_id = ?, rating = ?, rating_count = ?, balance = ?
        WHERE user_id = ?
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, seller.getShopName());
      ps.setString(2, seller.getCitizenId());
      ps.setDouble(3, seller.getRating());
      ps.setInt(4, seller.getRatingCount());
      ps.setDouble(5, seller.getBalance());
      ps.setString(6, seller.getId());
      ps.executeUpdate();
    }
  }

  /** Cập nhật thông tin Bidder (deposit_balance, total_bids...) */
  public void updateBidderDetails(Bidder bidder) throws SQLException {
    String sql = """
        UPDATE bidder_details
        SET deposit_balance = ?, frozen_balance = ?, shipping_address = ?, total_bids_placed = ?
        WHERE user_id = ?
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setDouble(1, bidder.getDepositBalance());
      ps.setDouble(2, bidder.getFrozenBalance());
      ps.setString(3, bidder.getShippingAddress());
      ps.setInt(4, bidder.getTotalBidsPlaced());
      ps.setString(5, bidder.getId());
      ps.executeUpdate();
    }
  }

  /**
   * Khoá hoặc mở khoá tài khoản (Admin thực hiện).
   *
   * @param userId ID user cần thay đổi
   * @param active true = mở khoá, false = khoá
   */
  public void setActive(String userId, boolean active) throws SQLException {
    String sql = "UPDATE users SET active = ? WHERE id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setBoolean(1, active);
      ps.setString(2, userId);
      ps.executeUpdate();
    }
  }

  /**
   * Nâng cấp tài khoản Bidder thành Seller.
   * Cập nhật role trong bảng users và thêm record vào seller_details.
   * Giữ nguyên bidder_details (vì Seller kế thừa Bidder).
   */
  public void upgradeToSeller(String userId, String shopName, String citizenId) throws SQLException {
    // 1. Lấy số dư deposit từ bidder_details trước khi nâng cấp
    double depositBalance = 0.0;
    String sqlBidderBalance = "SELECT deposit_balance FROM bidder_details WHERE user_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sqlBidderBalance)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          depositBalance = rs.getDouble("deposit_balance");
        }
      }
    }

    // 2. Update Role in users table
    String sqlUser = "UPDATE users SET role = 'SELLER' WHERE id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sqlUser)) {
      ps.setString(1, userId);
      ps.executeUpdate();
    }

    // 3. Insert into seller_details (copy deposit_balance từ bidder_details vào balance)
    String sqlSeller = "INSERT INTO seller_details (user_id, shop_name, citizen_id, rating, rating_count, balance) VALUES (?, ?, ?, 0.0, 0, ?)";
    try (PreparedStatement ps = getConnection().prepareStatement(sqlSeller)) {
      ps.setString(1, userId);
      ps.setString(2, shopName);
      ps.setString(3, citizenId);
      ps.setDouble(4, depositBalance);
      ps.executeUpdate();
    }
  }

  // -------------------------------------------------------
  // PRIVATE HELPER — Chuyển đổi ResultSet → User object
  // -------------------------------------------------------

  /**
   * Map một hàng (row) trong ResultSet thành User object.
   * Đây là điểm kết nối giữa world của database (SQL) và world của Java (OOP).
   *
   * 🎓 ResultSet là gì?
   *   Kết quả trả về từ câu query SQL. Giống như con trỏ duyệt từng hàng:
   *   rs.next() → di chuyển đến hàng tiếp theo
   *   rs.getString("column") → lấy giá trị cột theo tên
   *
   * @param rs ResultSet đang ở vị trí hàng cần đọc
   * @return đối tượng User đúng loại (Admin/Seller/Bidder)
   */
  private User mapRowToUser(ResultSet rs) throws SQLException {
    String id = rs.getString("id");
    String fullName = rs.getString("full_name");
    String username = rs.getString("username");
    String email = rs.getString("email");
    String passwordHash = rs.getString("password_hash");
    String phoneNumber = rs.getString("phone_number");
    String gender = rs.getString("gender");
    String dateOfBirth = rs.getString("date_of_birth");
    LocalDateTime createdAt =
      LocalDateTime.parse(rs.getString("created_at"));
    boolean active = rs.getBoolean("active");
    String role = rs.getString("role");

    // Dùng UserFactory để tạo đúng subclass dựa trên role
    // Factory Pattern kết hợp DAO Pattern ở đây!
    return switch (UserRole.fromString(role)) {
      case ADMIN -> {
        String adminNotes = findAdminNotes(id);
        yield UserFactory.createAdmin(id, fullName, username, email, passwordHash,
            phoneNumber, gender, dateOfBirth, createdAt, active, adminNotes);
      }
      case SELLER -> {
        SellerExtra extra = findSellerExtra(id);
        BidderExtra bidderExtra = findBidderExtra(id);
        yield UserFactory.createSeller(id, fullName, username, email, passwordHash,
            phoneNumber, gender, dateOfBirth, createdAt, active, 
            bidderExtra.depositBalance, bidderExtra.frozenBalance, bidderExtra.shippingAddress, bidderExtra.totalBids, 
            extra.shopName, extra.citizenId, extra.rating, extra.ratingCount, extra.balance);
      }
      case BIDDER -> {
        BidderExtra extra = findBidderExtra(id);
        yield UserFactory.createBidder(id, fullName, username, email, passwordHash,
            phoneNumber, gender, dateOfBirth, createdAt, active, extra.depositBalance, extra.frozenBalance, extra.shippingAddress, extra.totalBids);
      }
    };
  }

  /** Lấy adminNotes từ bảng admin_details */
  private String findAdminNotes(String userId) throws SQLException {
    String sql = "SELECT admin_notes FROM admin_details WHERE user_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getString("admin_notes") : "";
      }
    }
  }

  /** Record tạm để truyền extra data của Seller */
  private record SellerExtra(String shopName, String citizenId, double rating, int ratingCount, double balance) {}

  /** Lấy thông tin extra của Seller */
  private SellerExtra findSellerExtra(String userId) throws SQLException {
    String sql = "SELECT * FROM seller_details WHERE user_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new SellerExtra(
              rs.getString("shop_name"),
              rs.getString("citizen_id"),
              rs.getDouble("rating"),
              rs.getInt("rating_count"),
              rs.getDouble("balance")
          );
        }
      }
    }
    return new SellerExtra("Default Shop", "000000000000", 0.0, 0, 0.0);
  }

  /**
   * Lấy deposit_balance từ bidder_details cho một user.
   * Dùng để khôi phục số dư cho Seller nâng cấp từ Bidder.
   */
  public double getBidderDepositBalance(String userId) throws SQLException {
    String sql = "SELECT deposit_balance FROM bidder_details WHERE user_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getDouble("deposit_balance") : 0.0;
      }
    }
  }

  /**
   * Cập nhật seller_details.balance.
   * Dùng để đồng bộ số dư sau khi khôi phục cho Seller nâng cấp từ Bidder.
   */
  public void updateSellerBalance(String userId, double balance) throws SQLException {
    String sql = "UPDATE seller_details SET balance = ? WHERE user_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setDouble(1, balance);
      ps.setString(2, userId);
      ps.executeUpdate();
    }
  }

  /** Record tạm để truyền extra data của Bidder */
  private record BidderExtra(double depositBalance, double frozenBalance, String shippingAddress, int totalBids) {}

  /** Lấy thông tin extra của Bidder */
  private BidderExtra findBidderExtra(String userId) throws SQLException {
    String sql = "SELECT * FROM bidder_details WHERE user_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new BidderExtra(
              rs.getDouble("deposit_balance"),
              rs.getDouble("frozen_balance"),
              rs.getString("shipping_address"),
              rs.getInt("total_bids_placed")
          );
        }
      }
    }
    return new BidderExtra(0.0, 0.0, null, 0);
  }
}
