package com.auction.server.service;

import java.util.List;
import java.util.Optional;

import com.auction.enums.UserRole;
import com.auction.exception.BusinessException;
import com.auction.exception.DatabaseException;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;

/**
 * ============================================================
 * Class UserService — Quản lý thông tin & trạng thái User
 * ============================================================
 *
 * 🎯 SOLID — Single Responsibility Principle (SRP):
 * Class này CHỈ chịu trách nhiệm quản lý trạng thái người dùng
 * sau khi tài khoản đã được tạo ra.
 *
 * Các trách nhiệm KHÔNG thuộc class này (đã tách sang class riêng):
 *   ❌ Đăng ký        → {@link RegistrationService}
 *   ❌ Đăng nhập      → {@link AuthService}
 *   ❌ Hash mật khẩu  → {@link PasswordHasher}
 *
 * 📋 CÁC NGHIỆP VỤ THUỘC PHẠM VI NÀY:
 *   ✅ Khoá / mở khoá tài khoản (Admin thực hiện)
 *   ✅ Tìm kiếm user theo ID, email, role
 *   ✅ Nạp tiền vào ví Bidder
 *
 * 📐 KIẾN TRÚC 3 TẦNG:
 *   Handler/Controller → UserService → UserDAO → Database
 * ============================================================
 */
public class UserService {

  private final UserDAO userDAO;

  /**
   * Constructor Injection — nhận UserDAO từ ngoài.
   * Không tạo {@code new UserDAO()} ở đây → loose coupling, dễ mock khi test.
   */
  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  // -------------------------------------------------------
  // QUẢN LÝ TRẠNG THÁI (dành cho Admin)
  // -------------------------------------------------------

  /**
   * Khoá hoặc mở khoá tài khoản người dùng.
   *
   * <p>Quy tắc bảo vệ: Admin không được tự khoá chính mình
   * để tránh tình huống không ai có thể đăng nhập hệ thống.
   *
   * @param adminId  ID của Admin thực hiện thao tác
   * @param targetId ID của User cần khoá/mở khoá
   * @param active   {@code true} = mở khoá, {@code false} = khoá
   * @throws IllegalArgumentException nếu Admin cố tự khoá chính mình
   * @throws RuntimeException         nếu có lỗi database
   */
  public void setUserActive(String adminId, String targetId, boolean active) {
    if (adminId.equals(targetId)) {
      throw BusinessException.cannotLockOwnAccount();
    }
    try {
      userDAO.setActive(targetId, active);
      String action = active ? "Mở khoá" : "Khoá";
      System.out.println("[UserService] " + action + " user: " + targetId + " bởi Admin: " + adminId);
    } catch (Exception e) {
      throw DatabaseException.queryFailed("thay đổi trạng thái user", e);
    }
  }

  // -------------------------------------------------------
  // TÌM KIẾM & TRUY VẤN
  // -------------------------------------------------------

  /**
   * Tìm user theo ID.
   *
   * @param id ID cần tìm
   * @return {@link Optional} chứa User nếu tồn tại, rỗng nếu không tìm thấy
   * @throws RuntimeException nếu có lỗi database
   */
  public Optional<User> findById(String id) {
    try {
      return userDAO.findById(id);
    } catch (Exception e) {
      throw DatabaseException.queryFailed("tìm user", e);
    }
  }

  /**
   * Lấy tất cả user theo role — dành cho Admin quản lý.
   *
   * @param role vai trò cần lọc (ADMIN, SELLER, BIDDER)
   * @return danh sách {@link User} có role tương ứng
   * @throws RuntimeException nếu có lỗi database
   */
  public List<User> getUsersByRole(UserRole role) {
    try {
      return userDAO.findAllByRole(role);
    } catch (Exception e) {
      throw DatabaseException.queryFailed("truy vấn user theo role", e);
    }
  }

  /**
   * Lấy toàn bộ danh sách user — dành cho Admin quản lý.
   *
   * @return danh sách tất cả {@link User} trong hệ thống
   * @throws RuntimeException nếu có lỗi database
   */
  public List<User> getAllUsers() {
    try {
      return userDAO.findAll();
    } catch (Exception e) {
      throw DatabaseException.queryFailed("lấy toàn bộ user", e);
    }
  }

  // -------------------------------------------------------
  // QUẢN LÝ VÍ BIDDER
  // -------------------------------------------------------

  /**
   * Nạp tiền vào tài khoản Bidder.
   *
   * <p>Kiểm tra user phải tồn tại và có vai trò Bidder trước khi nạp.
   *
   * @param bidderId ID của Bidder
   * @param amount   số tiền cần nạp (VND, phải > 0)
   * @throws IllegalArgumentException nếu không tìm thấy Bidder với ID đã cho
   * @throws RuntimeException         nếu có lỗi database
   */
  public void depositForBidder(String bidderId, double amount) {
    try {
      Optional<User> opt = userDAO.findById(bidderId);
      if (opt.isEmpty() || !(opt.get() instanceof Bidder bidder)) {
        throw DatabaseException.notFound("Bidder", bidderId);
      }
      bidder.setBalance(bidder.getBalance() + amount);
      userDAO.updateBidderDetails(bidder);
      System.out.printf("[UserService] Nạp %.0f VND cho Bidder: %s%n",
          amount, bidder.getFullName());
    } catch (Exception e) {
      throw DatabaseException.queryFailed("nạp tiền", e);
    }
  }
}
