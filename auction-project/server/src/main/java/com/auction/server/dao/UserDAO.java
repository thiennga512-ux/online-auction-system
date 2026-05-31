package com.auction.server.dao;

import com.auction.factory.UserFactory;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.enums.UserRole;
import com.auction.server.database.DatabaseManager;
import com.auction.exception.DatabaseException;
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
 * DAO (Data Access Object) xử lý tương tác DB.
 * Đã refactor sử dụng DatabaseException thay cho SQLException.
 * ============================================================
 */
public class UserDAO {

  /** Lấy connection qua Singleton DatabaseManager */
  private Connection getConnection() {
    try {
      return DatabaseManager.getInstance().getConnection();
    } catch (Exception e) {
      throw DatabaseException.connectionFailed(e);
    }
  }

  // -------------------------------------------------------
  // CREATE — Lưu User mới vào database
  // -------------------------------------------------------

  public void save(User user) {
    String sql = """
        INSERT INTO users (id, full_name, username, email, password_hash,
                           phone_number, gender, date_of_birth, created_at, active, role)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.getId());
      ps.setString(2, user.getFullName());
      ps.setString(3, user.getUsername());
      ps.setString(4, user.getEmail());
      ps.setString(5, user.getPasswordHash());
      ps.setString(6, user.getPhoneNumber());
      ps.setString(7, user.getGender());
      ps.setString(8, user.getDateOfBirth());
      ps.setString(9, user.getCreatedAt() != null ? user.getCreatedAt().toString() : LocalDateTime.now().toString());
      ps.setBoolean(10, user.isActive());
      ps.setString(11, user.getRole().name());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("lưu user", e);
    }

    switch (user.getRole()) {
      case ADMIN -> saveAdminDetails((Admin) user);
      case SELLER -> saveSellerDetails((Seller) user);
      case BIDDER -> saveBidderDetails((Bidder) user);
    }
  }

  private void saveAdminDetails(Admin admin) {
    String sql = "INSERT INTO admin_details (user_id, admin_notes) VALUES (?, ?)";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, admin.getId());
      ps.setString(2, "");
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("lưu chi tiết admin", e);
    }
  }

  private void saveSellerDetails(Seller seller) {
    String sql = """
        INSERT INTO seller_details (user_id, shop_name, citizen_id, balance)
        VALUES (?, ?, ?, ?)
        """;
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, seller.getId());
      ps.setString(2, ""); 
      ps.setString(3, ""); 
      ps.setDouble(4, seller.getBalance());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("lưu chi tiết seller", e);
    }
  }

  private void saveBidderDetails(Bidder bidder) {
    String sql = """
        INSERT INTO bidder_details (user_id, deposit_balance, frozen_balance, shipping_address, total_bids_placed)
        VALUES (?, ?, ?, ?, ?)
        """;
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, bidder.getId());
      ps.setDouble(2, bidder.getBalance()); // Bidder uses balance for deposit
      ps.setDouble(3, bidder.getFrozenBalance());
      ps.setString(4, bidder.getShippingAddress() != null ? bidder.getShippingAddress() : "");
      ps.setInt(5, 0); 
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("lưu chi tiết bidder", e);
    }
  }

  // -------------------------------------------------------
  // READ — Đọc User từ database
  // -------------------------------------------------------

  public Optional<User> findById(String id) {
    String sql = "SELECT * FROM users WHERE id = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToUser(rs));
        }
      }
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("tìm user theo id", e);
    }
    return Optional.empty();
  }

  public Optional<User> findByEmail(String email) {
    String sql = "SELECT * FROM users WHERE email = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToUser(rs));
        }
      }
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("tìm user theo email", e);
    }
    return Optional.empty();
  }

  public List<User> findAllByRole(UserRole role) {
    List<User> result = new ArrayList<>();
    String sql = "SELECT * FROM users WHERE role = ? AND active = 1";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, role.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRowToUser(rs));
        }
      }
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("tìm user theo role", e);
    }
    return result;
  }

  public List<User> findAll() {
    List<User> result = new ArrayList<>();
    String sql = "SELECT * FROM users ORDER BY created_at DESC";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRowToUser(rs));
        }
      }
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("lấy tất cả user", e);
    }
    return result;
  }

  public int count() {
    String sql = "SELECT COUNT(*) FROM users";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("đếm user", e);
    }
  }

  public boolean emailExists(String email) {
    String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("kiểm tra email", e);
    }
  }

  public boolean citizenIdExists(String citizenId) {
    String sql = "SELECT COUNT(*) FROM seller_details WHERE citizen_id = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, citizenId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("kiểm tra CCCD", e);
    }
  }

  // -------------------------------------------------------
  // UPDATE — Cập nhật thông tin User
  // -------------------------------------------------------

  public void update(User user) {
    String sql = """
        UPDATE users
        SET full_name = ?, username = ?, email = ?, phone_number = ?, gender = ?, date_of_birth = ?, active = ?
        WHERE id = ?
        """;
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.getFullName());
      ps.setString(2, user.getUsername());
      ps.setString(3, user.getEmail());
      ps.setString(4, user.getPhoneNumber());
      ps.setString(5, user.getGender());
      ps.setString(6, user.getDateOfBirth());
      ps.setBoolean(7, user.isActive());
      ps.setString(8, user.getId());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("cập nhật user", e);
    }

    switch (user.getRole()) {
      case SELLER -> updateSellerDetails((Seller) user);
      case BIDDER -> updateBidderDetails((Bidder) user);
      case ADMIN -> { /* Admin ít thay đổi, bỏ qua */ }
    }
  }

  public void updateSellerDetails(Seller seller) {
    // Chỉ cập nhật các field mà Seller object thực sự quản lý
    String sql = """
        UPDATE seller_details
        SET balance = ?
        WHERE user_id = ?
        """;
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setDouble(1, seller.getBalance());
      ps.setString(2, seller.getId());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("cập nhật chi tiết seller", e);
    }
  }

  public void updateBidderDetails(Bidder bidder) {
    if (bidder instanceof Seller seller) {
      updateSellerDetails(seller);
      String sql = """
          UPDATE bidder_details
          SET frozen_balance = ?, shipping_address = ?
          WHERE user_id = ?
          """;
      try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setDouble(1, bidder.getFrozenBalance());
        ps.setString(2, bidder.getShippingAddress() != null ? bidder.getShippingAddress() : "");
        ps.setString(3, bidder.getId());
        ps.executeUpdate();
      } catch (SQLException e) {
        throw DatabaseException.queryFailed("cập nhật chi tiết bidder cho seller", e);
      }
      return;
    }
    // Chỉ cập nhật các field mà Bidder object thực sự quản lý
    String sql = """
        UPDATE bidder_details
        SET deposit_balance = ?, frozen_balance = ?, shipping_address = ?
        WHERE user_id = ?
        """;
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setDouble(1, bidder.getBalance());
      ps.setDouble(2, bidder.getFrozenBalance());
      ps.setString(3, bidder.getShippingAddress() != null ? bidder.getShippingAddress() : "");
      ps.setString(4, bidder.getId());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("cập nhật chi tiết bidder", e);
    }
  }

  public void setActive(String userId, boolean active) {
    String sql = "UPDATE users SET active = ? WHERE id = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setBoolean(1, active);
      ps.setString(2, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("cập nhật trạng thái active", e);
    }
  }

  public void upgradeToSeller(String userId, String shopName, String citizenId) {
    String sqlUser = "UPDATE users SET role = 'SELLER' WHERE id = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sqlUser)) {
      ps.setString(1, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("nâng cấp role thành SELLER", e);
    }

    String sqlSeller = "INSERT INTO seller_details (user_id, shop_name, citizen_id, balance) VALUES (?, ?, ?, 0.0)";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sqlSeller)) {
      ps.setString(1, userId);
      ps.setString(2, shopName);
      ps.setString(3, citizenId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("thêm thông tin seller_details", e);
    }
  }

  // -------------------------------------------------------
  // PRIVATE HELPER — Chuyển đổi ResultSet → User object
  // -------------------------------------------------------

  private User mapRowToUser(ResultSet rs) throws SQLException {
    String id = rs.getString("id");
    String fullName = rs.getString("full_name");
    String username = rs.getString("username");
    String email = rs.getString("email");
    String passwordHash = rs.getString("password_hash");
    String phoneNumber = rs.getString("phone_number");
    String gender = rs.getString("gender");
    String dateOfBirth = rs.getString("date_of_birth");
    
    String createdAtStr = rs.getString("created_at");
    LocalDateTime createdAt = createdAtStr != null ? LocalDateTime.parse(createdAtStr) : LocalDateTime.now();
    boolean active = rs.getBoolean("active");
    String role = rs.getString("role");

    return switch (UserRole.fromString(role)) {
      case ADMIN -> {
        Admin admin = UserFactory.rebuildAdmin(id, createdAt, createdAt, username, passwordHash, email, fullName, active, gender, dateOfBirth);
        admin.setPhoneNumber(phoneNumber);
        yield admin;
      }
      case SELLER -> {
        SellerExtra extra = findSellerExtra(id);
        Seller seller = UserFactory.rebuildSeller(id, createdAt, createdAt, username, passwordHash, email, fullName, active, gender, dateOfBirth, extra.balance, extra.frozenBalance, extra.shippingAddress, extra.shopName, extra.citizenId);
        seller.setPhoneNumber(phoneNumber);
        yield seller;
      }
      case BIDDER -> {
        BidderExtra extra = findBidderExtra(id);
        Bidder bidder = UserFactory.rebuildBidder(id, createdAt, createdAt, username, passwordHash, email, fullName, active, gender, dateOfBirth, extra.depositBalance, extra.frozenBalance, extra.shippingAddress);
        bidder.setPhoneNumber(phoneNumber);
        yield bidder;
      }
    };
  }

  private record SellerExtra(String shopName, String citizenId, double balance, double frozenBalance, String shippingAddress) {}

  private SellerExtra findSellerExtra(String userId) throws SQLException {
    String sql = """
        SELECT sd.*, bd.frozen_balance, bd.shipping_address
        FROM seller_details sd
        LEFT JOIN bidder_details bd ON sd.user_id = bd.user_id
        WHERE sd.user_id = ?
        """;
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new SellerExtra(
              rs.getString("shop_name"),
              rs.getString("citizen_id"),
              rs.getDouble("balance"),
              rs.getDouble("frozen_balance"),
              rs.getString("shipping_address")
          );
        }
      }
    }
    return new SellerExtra("Default Shop", "000000000000", 0.0, 0.0, null);
  }

  private record BidderExtra(double depositBalance, double frozenBalance, String shippingAddress, int totalBids) {}

  private BidderExtra findBidderExtra(String userId) throws SQLException {
    String sql = "SELECT * FROM bidder_details WHERE user_id = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

  public boolean usernameExists(String username) {
    String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("kiểm tra username", e);
    }
  }
}