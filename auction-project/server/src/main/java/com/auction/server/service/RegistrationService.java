package com.auction.server.service;

import com.auction.exception.ValidationException;
import com.auction.exception.BusinessException;
import com.auction.exception.DatabaseException;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.enums.UserRole;
import com.auction.factory.UserFactory;
import com.auction.server.dao.UserDAO;
import java.util.Optional;

/**
 * ============================================================
 * Class RegistrationService — Xử lý logic Đăng Ký tài khoản
 * ============================================================
 *
 * 🎯 SOLID — Single Responsibility Principle (SRP):
 * Class này CHỈ chịu trách nhiệm đăng ký và nâng cấp tài khoản.
 * Không xử lý đăng nhập, không quản lý user sau đăng ký.
 *
 * 📬 Email chỉ được dùng ở đây cho mục đích đăng ký (email chào mừng).
 * Thông báo nghiệp vụ (bán sản phẩm, bid mới...) được xử lý qua
 * chuông socket trong NotificationService, KHÔNG dùng email.
 *
 * 📋 CÁC NGHIỆP VỤ THUỘC PHẠM VI NÀY:
 * - Đăng ký tài khoản mới (mặc định Bidder)
 * - Tạo tài khoản Admin (nội bộ)
 * - Nâng cấp Bidder → Seller
 * - Validate input đăng ký
 * - Gửi email chào mừng sau đăng ký
 *
 * 🔒 QUY TẮc NGHIỆP VỤ:
 * - Mọi user mới đăng ký đều là Bidder
 * - Chỉ Bidder mới có thể nâng cấp lên Seller
 * - Email phải unique trong toàn hệ thống
 * - Số CCCD phải unique và đúng định dạng 9-12 chữ số
 * ============================================================
 */
public class RegistrationService {

  private final UserDAO userDAO;

  /**
   * Constructor Injection — nhận UserDAO từ ngoài.
   * Không tạo {@code new UserDAO()} ở đây → loose coupling, dễ mock khi test.
   */
  public RegistrationService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  // -------------------------------------------------------
  // ĐĂNG KÝ TÀI KHOẢN MỚI
  // -------------------------------------------------------

  /**
   * Đăng ký tài khoản mới — mặc định là Bidder (người đặt giá).
   *
   * <p>
   * Mọi người dùng mới đều bắt đầu với vai trò Bidder.
   * Muốn trở thành Seller phải gọi {@link #upgradeToSeller}.
   *
   * @param fullName họ và tên đầy đủ
   * @param username tên đăng nhập (unique)
   * @param email    email đăng nhập (phải unique)
   * @param password mật khẩu gốc (chưa hash, tối thiểu 6 ký tự)
   * @return {@link Bidder} vừa được tạo
   * @throws IllegalArgumentException nếu email đã tồn tại hoặc dữ liệu không hợp
   *                                  lệ
   * @throws RuntimeException         nếu có lỗi database
   */
  public Bidder registerUser(String fullName, String username, String email,
      String password) {
    validateRegistrationInput(fullName, username, email, password);
    return (Bidder) registerInternal(fullName, username, email, password, UserRole.BIDDER);
  }

  /**
   * Đăng ký Bidder nhanh — dùng trong demo/test.
   * Tự động sinh username từ email.
   */
  public Bidder registerBidder(String fullName, String email, String password) {
    return registerUser(fullName, email.split("@")[0], email, password);
  }

  public Admin createAdmin(String fullName, String username, String email,
      String password) {
    validateRegistrationInput(fullName, username, email, password);
    return (Admin) registerInternal(fullName, username, email, password, UserRole.ADMIN);
  }

  /** Tạo Admin nhanh — dùng trong demo/test. */
  public Admin createAdmin(String fullName, String email, String password) {
    return createAdmin(fullName, email.split("@")[0], email, password);
  }

  // -------------------------------------------------------
  // NÂNG CẤP TÀI KHOẢN
  // -------------------------------------------------------

  /**
   * Nâng cấp tài khoản Bidder → Seller sau khi xác minh thông tin.
   *
   * <p>
   * Quy tắc nghiệp vụ:
   * <ul>
   * <li>Tài khoản phải đang là Bidder (không nâng cấp chồng lên Seller)</li>
   * <li>Tên cửa hàng ít nhất 3 ký tự</li>
   * <li>Số CCCD phải là 9–12 chữ số và chưa được dùng bởi Seller khác</li>
   * </ul>
   *
   * @param userId    ID của Bidder muốn nâng cấp
   * @param shopName  tên cửa hàng
   * @param citizenId số CCCD (9–12 chữ số)
   * @return {@link Seller} vừa được nâng cấp
   * @throws IllegalArgumentException nếu vi phạm quy tắc nghiệp vụ
   * @throws RuntimeException         nếu có lỗi database
   */
  public Seller upgradeToSeller(String userId, String shopName, String citizenId) {
    String normalizedShopName = shopName == null ? "" : shopName.trim();
    String normalizedCitizenId = citizenId == null ? "" : citizenId.trim();

    if (normalizedShopName.isBlank())
      throw ValidationException.required("Tên cửa hàng");
    if (normalizedShopName.length() < 3)
      throw ValidationException.tooShort("Tên cửa hàng", 3);
    if (!normalizedCitizenId.matches("\\d{9,12}"))
      throw new ValidationException("citizenId", "Số CCCD chỉ gồm 9-12 chữ số.");

    try {
      if (userDAO.citizenIdExists(normalizedCitizenId)) {
        throw BusinessException.citizenIdAlreadyUsed();
      }

      Optional<User> optUser = userDAO.findById(userId);
      if (optUser.isEmpty()) {
        throw DatabaseException.notFound("User", userId);
      }
      User user = optUser.get();
      if (user.getRole() == UserRole.SELLER)
        throw BusinessException.alreadySeller();
      if (user.getRole() != UserRole.BIDDER)
        throw BusinessException.canOnlyUpgradeFromBidder();

      userDAO.upgradeToSeller(userId, normalizedShopName, normalizedCitizenId);

      System.out.println("[RegistrationService] Đã nâng cấp thành Seller: " + user.getFullName());

      // Load lại từ DB để có object Seller đầy đủ
      return (Seller) userDAO.findById(userId).orElseThrow();

    } catch (Exception e) {
      throw DatabaseException.queryFailed("nâng cấp Seller", e);
    }
  }

  /**
   * Đăng ký Seller nhanh — dùng trong demo/test.
   * Tạo Bidder trước rồi nâng cấp ngay lên Seller.
   */
  public Seller registerSeller(String fullName, String email, String password, String shopName) {
    Bidder bidder = registerBidder(fullName, email, password);
    // Sinh số CCCD giả định không trùng lặp dựa trên email
    String citizenId = String.format("%012d", Math.abs(email.hashCode() % 1000000000000L));
    return upgradeToSeller(bidder.getId(), shopName, citizenId);
  }

  // -------------------------------------------------------
  // PRIVATE HELPERS
  // -------------------------------------------------------

  /**
   * Method nội bộ — tạo user theo role bất kỳ, hash password và lưu vào DB.
   */
  private User registerInternal(String fullName, String username, String email,
      String password, UserRole role) {
    String passwordHash = PasswordHasher.hash(password);
    User user = UserFactory.create(role, username, passwordHash, email, fullName);
    saveUser(user);
    return user;
  }

  /** Lưu user vào DB, bắt SQLException thành RuntimeException. */
  private void saveUser(User user) {
    try {
      userDAO.save(user);
      System.out.println("[RegistrationService] Đã tạo user: " + user);
    } catch (Exception e) {
      throw DatabaseException.queryFailed("lưu user", e);
    }
  }

  /**
   * Kiểm tra tính hợp lệ của input đăng ký.
   *
   * <p>
   * Validate theo thứ tự: họ tên → username → email → mật khẩu → ngày sinh →
   * email unique.
   *
   * @throws IllegalArgumentException khi bất kỳ điều kiện nào không thoả mãn
   */
  private void validateRegistrationInput(String fullName, String username,
      String email, String password) {
    if (fullName == null || fullName.isBlank())
      throw ValidationException.required("Họ tên");
    if (username == null || username.isBlank())
      throw ValidationException.required("Tên đăng nhập");
    if (email == null || !email.contains("@") || !email.contains("."))
      throw ValidationException.invalidEmail(email);
    if (password == null || password.length() < 6)
      throw ValidationException.passwordTooShort(6);

    // Kiểm tra email đã tồn tại chưa (phải gọi DB)
    try {
      if (userDAO.emailExists(email.toLowerCase().trim())) {
        throw BusinessException.emailAlreadyRegistered(email);
      }
      if (userDAO.usernameExists(username.toLowerCase().trim())) {
        throw BusinessException.usernameAlreadyTaken(username);
      }
    } catch (Exception e) {
      throw DatabaseException.queryFailed("kiểm tra email", e);
    }
  }
}
