package com.auction.common.factory;

import com.auction.common.model.user.Admin;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.User;
import com.auction.common.model.user.UserRole;
import java.time.LocalDateTime;
public class UserFactory {

  private UserFactory() {
    throw new UnsupportedOperationException("UserFactory là Utility Class, không tạo instance được");
  }

  // -------------------------------------------------------
  // FACTORY METHODS — Tạo User mới
  // -------------------------------------------------------

  /**
   * Tạo User mới dựa trên vai trò (role).
   * Dùng khi người dùng ĐĂNG KÝ tài khoản mới.
   *
   * @param role         vai trò: "ADMIN", "SELLER", hoặc "BIDDER"
   * @param fullName     họ tên
   * @param email        email đăng nhập
   * @param passwordHash mật khẩu đã hash
   * @return đối tượng User tương ứng với vai trò
   * @throws IllegalArgumentException nếu role không hợp lệ
   */
  public static User create(String role, String fullName, String username, String email, String passwordHash, String gender, String dateOfBirth) {
    // Chuyển String → UserRole (kiểm tra luôn nếu sai)
    UserRole userRole = UserRole.fromString(role);
    return create(userRole, fullName, username, email, passwordHash, gender, dateOfBirth);
  }

  /**
   * Overload method — nhận UserRole thay vì String.
   * Dùng khi code đã có sẵn UserRole object.
   */
  public static User create(UserRole role, String fullName, String username, String email, String passwordHash, String gender, String dateOfBirth) {
    // Dùng switch expression (Java 14+) — gọn hơn if-else
    return switch (role) {
      case ADMIN -> new Admin(fullName, username, email, passwordHash, gender, dateOfBirth);
      case SELLER -> {
        // Seller cần thêm shopName và citizenId, dùng tên mặc định để chống null khi tạo từ role SELLER
        yield new Seller(fullName, username, email, passwordHash, gender, dateOfBirth, fullName + "'s Shop", "000000000000");
      }
      case BIDDER -> new Bidder(fullName, username, email, passwordHash, gender, dateOfBirth);
    };
  }

  // -------------------------------------------------------
  // FACTORY METHODS — Tạo User TỪ DATABASE (đã có ID)
  // -------------------------------------------------------

  /**
   * Tạo lại đối tượng Admin từ dữ liệu đọc ra từ SQLite.
   * Called by UserDAO khi load từ database.
   */
  public static Admin createAdmin(String id, String fullName, String username, String email,
      String passwordHash, String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt,
      boolean active, String adminNotes) {
    return new Admin(id, fullName, username, email, passwordHash, phoneNumber, gender, dateOfBirth,
        createdAt, active, adminNotes);
  }

  /**
   * Tạo lại đối tượng Seller từ dữ liệu SQLite.
   */
  public static Seller createSeller(String id, String fullName, String username, String email,
      String passwordHash, String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt,
      boolean active, double depositBalance, double frozenBalance, String shippingAddress, int totalBidsPlaced,
      String shopName, String citizenId, double rating, int ratingCount, double balance) {
    return new Seller(id, fullName, username, email, passwordHash, phoneNumber, gender, dateOfBirth,
        createdAt, active, depositBalance, frozenBalance, shippingAddress, totalBidsPlaced,
        shopName, citizenId, rating, ratingCount, balance);
  }

  /**
   * Tạo lại đối tượng Bidder từ dữ liệu SQLite.
   */
  public static Bidder createBidder(String id, String fullName, String username, String email,
      String passwordHash, String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt,
      boolean active, double depositBalance, double frozenBalance, String shippingAddress, int totalBidsPlaced) {
    return new Bidder(id, fullName, username, email, passwordHash, phoneNumber, gender, dateOfBirth,
        createdAt, active, depositBalance, frozenBalance, shippingAddress, totalBidsPlaced, UserRole.BIDDER);
  }

  /**
   * Tổng quát: reconstruct User từ database dựa trên role string.
   * Dùng trong UserDAO khi query từ bảng users.
   *
   * 🎓 Lý do cần method này:
   * Khi đọc từ SQLite, ta có một row với cột "role" = "BIDDER".
   * Method này quyết định tạo đúng subclass tương ứng.
   */
  public static User reconstruct(String role, String id, String fullName, String username, String email,
      String passwordHash, String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt, boolean active) {
    UserRole userRole = UserRole.fromString(role);
    return switch (userRole) {
      case ADMIN -> createAdmin(id, fullName, username, email, passwordHash,
          phoneNumber, gender, dateOfBirth, createdAt, active, "");
      case SELLER -> createSeller(id, fullName, username, email, passwordHash,
          phoneNumber, gender, dateOfBirth, createdAt, active, 0.0, 0.0, null, 0, fullName + "'s Shop", "000000000000", 0.0, 0, 0.0);
      case BIDDER -> createBidder(id, fullName, username, email, passwordHash,
          phoneNumber, gender, dateOfBirth, createdAt, active, 0.0, 0.0, null, 0);
    };
  }
}
