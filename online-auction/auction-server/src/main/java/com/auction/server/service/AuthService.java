package com.auction.server.service;

import com.auction.common.exception.AuthException;
import com.auction.common.exception.DatabaseException;
import com.auction.common.exception.ValidationException;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDAO;
import java.sql.SQLException;
import java.util.Optional;

/**
 * ============================================================
 * Class AuthService — Xử lý logic Đăng Nhập (Authentication)
 * ============================================================
 *
 * 🎯 SOLID — Single Responsibility Principle (SRP):
 * Class này CHỈ chịu trách nhiệm xác thực danh tính người dùng.
 * Không xử lý đăng ký, không quản lý user, không liên quan DAO khác.
 *
 * 🔑 AUTHENTICATION vs AUTHORIZATION:
 *   Authentication (Xác thực) — "Bạn là ai?" — do class này xử lý
 *   Authorization  (Phân quyền) — "Bạn được làm gì?" — do hasPermission() trong User
 *
 * 🔒 BẢO MẬT:
 *   - KHÔNG bao giờ tiết lộ "email không tồn tại" → bảo vệ khỏi user enumeration attack
 *   - Mật khẩu được hash trước khi so sánh
 *   - Kiểm tra trạng thái active sau khi xác thực thành công
 * ============================================================
 */
public class AuthService {

  private final UserDAO userDAO;

  /**
   * Constructor Injection — nhận UserDAO từ ngoài.
   * Không tạo {@code new UserDAO()} ở đây → loose coupling, dễ mock khi test.
   */
  public AuthService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  /**
   * Xác thực đăng nhập bằng email và mật khẩu gốc.
   *
   * <p>Flow xử lý:
   * <ol>
   *   <li>Validate input cơ bản (không rỗng)</li>
   *   <li>Tìm user theo email (case-insensitive)</li>
   *   <li>Hash mật khẩu nhập vào và so sánh với hash đã lưu</li>
   *   <li>Kiểm tra tài khoản còn active không</li>
   * </ol>
   *
   * @param email    email đăng nhập
   * @param password mật khẩu gốc (plain text, chưa hash)
   * @return {@link User} nếu đăng nhập thành công
   * @throws ValidationException nếu email/password rỗng
   * @throws AuthException       nếu sai thông tin hoặc tài khoản bị khoá
   * @throws DatabaseException   nếu có lỗi database
   */
  public User login(String email, String password) {
    // Bước 1 — Validate input cơ bản
    if (email == null || email.isBlank())    throw ValidationException.required("Email");
    if (password == null || password.isBlank()) throw ValidationException.required("Mật khẩu");

    try {
      // Bước 2 — Tìm user theo email (normalize: lowercase + trim)
      Optional<User> optUser = userDAO.findByEmail(email.toLowerCase().trim());

      if (optUser.isEmpty()) {
        // Không tiết lộ "email không tồn tại" để chống user enumeration attack
        throw AuthException.invalidCredentials();
      }

      User user = optUser.get();

      // Bước 3 — So sánh hash mật khẩu
      String inputHash = PasswordHasher.hash(password);
      if (!user.checkPassword(inputHash)) {
        throw AuthException.invalidCredentials();
      }

      // Bước 4 — Kiểm tra tài khoản còn hoạt động
      if (!user.isActive()) {
        throw AuthException.accountLocked();
      }

      System.out.println("[AuthService] Đăng nhập thành công: " + user);
      return user;

    } catch (SQLException e) {
      throw DatabaseException.queryFailed("đăng nhập", e);
    }
  }
}
