package com.auction.common.model.user;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ============================================================
 * Abstract Class User — Lớp CHA trừu tượng cho mọi loại người dùng
 * ============================================================
 *
 * 🎓 TẠI SAO DÙNG ABSTRACT CLASS?
 *
 * Trong thực tế, KHÔNG AI chỉ là "User" chung chung cả.
 * Mọi người dùng đều có vai trò cụ thể: Admin, Seller, hoặc Bidder.
 * → Dùng từ khoá "abstract" ngăn code tạo `new User()` trực tiếp.
 * → Buộc lập trình viên phải tạo đúng loại: `new Admin(...)`.
 *
 * 🔑 4 TÍNH CHẤT OOP THỂ HIỆN Ở ĐÂY:
 *
 * 1. TRỪU TƯỢNG (Abstraction):
 * - Class User là abstract → không thể khởi tạo trực tiếp
 * - Method getRole() là abstract → mỗi subclass PHẢI tự implement
 * → Che giấu chi tiết triển khai, chỉ lộ ra "hợp đồng" (contract)
 *
 * 2. ĐÓNG GÓI (Encapsulation):
 * - Tất cả field đều là PRIVATE
 * - Chỉ truy cập/thay đổi qua getter/setter
 * - passwordHash là private, không bao giờ lộ ra ngoài
 * → Kiểm soát chặt dữ liệu, tránh sửa sai từ bên ngoài
 *
 * 3. KẾ THỪA (Inheritance):
 * - Admin, Seller, Bidder đều EXTENDS User
 * - Tự động có id, name, email không cần viết lại
 * → Tái sử dụng code, giảm lặp lại (DRY principle)
 *
 * 4. ĐA HÌNH (Polymorphism):
 * - List<User> users = new ArrayList<>();
 * users.add(new Admin(...));
 * users.add(new Bidder(...));
 * - Duyệt danh sách: user.getRole() → kết quả khác nhau tùy loại
 * → Xử lý nhiều loại đối tượng qua cùng một interface
 * ============================================================
 */
public abstract class User {
  private final String id;
  private String fullName;
  private String email;
  private String username;
  private String gender;
  private String dateOfBirth;
  private String passwordHash;
  private String phoneNumber;
  private final LocalDateTime createdAt;
  private final UserRole role;

  /** Trạng thái tài khoản: true = hoạt động, false = bị khoá */
  private boolean active;

  /** Constructor cho GSON deserialize */
  protected User() {
    this.id = null;
    this.createdAt = null;
    this.role = null;
  }

  /**
   * Constructor đầy đủ — dùng khi tải từ database (đã có ID sẵn).
   */
  protected User(String id, String fullName, String username, String email, String passwordHash,
      String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt, boolean active, UserRole role) {
    this.id = id;
    this.fullName = fullName;
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.phoneNumber = phoneNumber;
    this.gender = gender;
    this.dateOfBirth = dateOfBirth;
    this.createdAt = createdAt;
    this.active = active;
    this.role = role;
  }

  /**
   * Constructor tạo mới — tự động sinh ID và gán thời gian hiện tại.
   * Dùng khi người dùng đăng ký tài khoản mới.
   */
  protected User(String fullName, String username, String email, String passwordHash, String gender,
      String dateOfBirth) {
    this(fullName, username, email, passwordHash, gender, dateOfBirth, UserRole.BIDDER);
  }

  /**
   * Constructor tạo mới với role chỉ định — dùng cho Admin và Seller.
   */
  protected User(String fullName, String username, String email, String passwordHash, String gender,
      String dateOfBirth, UserRole role) {
    this(
        UUID.randomUUID().toString(), // Sinh ID ngẫu nhiên
        fullName,
        username,
        email,
        passwordHash,
        null, // phoneNumber
        gender,
        dateOfBirth,
        LocalDateTime.now(),
        true, // Mặc định tài khoản mới là active
        role
    );
  }

  public UserRole getRole() {
    return role;
  }

  /**
   * 🔑 ĐA HÌNH — Mỗi loại user có dashboard khác nhau.
   * Dùng trong Giai đoạn 4 để điều hướng sau khi đăng nhập.
   *
   * @return tên màn hình FXML cần hiển thị
   */
  public abstract String getDashboardView();

  // -------------------------------------------------------
  // CONCRETE METHODS — Logic dùng chung cho mọi loại User
  // -------------------------------------------------------

  /**
   * Kiểm tra mật khẩu.
   * So sánh hash của mật khẩu nhập vào với hash đã lưu.
   *
   * @param hashedInput mật khẩu đã hash từ phía client
   * @return true nếu khớp
   */
  public boolean checkPassword(String hashedInput) {
    return this.passwordHash.equals(hashedInput);
  }

  /**
   * Kiểm tra xem user có quyền cao hơn hoặc bằng mức yêu cầu không.
   * Ví dụ: requireLevel(2) → true nếu là SELLER hoặc ADMIN
   *
   * @param requiredLevel mức quyền tối thiểu cần có
   * @return true nếu đủ quyền
   */
  public boolean hasPermission(int requiredLevel) {
    return getRole().getLevel() >= requiredLevel;
  }

  /**
   * Trả về thông tin ngắn gọn để hiển thị trên UI hoặc log.
   * Override toString() là best practice trong Java.
   */
  @Override
  public String toString() {
    return String.format("[%s] %s (%s)", getRole().name(), username != null ? username : fullName, email);
  }

  /**
   * Hai User được coi là BẰNG NHAU nếu có cùng ID.
   * Quan trọng khi dùng trong List, Set, Map.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof User other))
      return false;
    return this.id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  // -------------------------------------------------------
  // GETTERS & SETTERS — Đóng gói truy cập field
  // -------------------------------------------------------

  public String getId() {
    return id;
  }

  public String getFullName() {
    return fullName;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getGender() {
    return gender;
  }

  public String getDateOfBirth() {
    return dateOfBirth;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isActive() {
    return active;
  }

  public void setFullName(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("Họ tên không được để trống");
    }
    this.fullName = fullName;
  }

  public void setEmail(String email) {
    // Validation đơn giản — Giai đoạn 5 sẽ dùng regex đầy đủ
    if (email == null || !email.contains("@")) {
      throw new IllegalArgumentException("Email không hợp lệ: " + email);
    }
    this.email = email;
  }

  public void setPasswordHash(String passwordHash) {
    if (passwordHash == null || passwordHash.isBlank()) {
      throw new IllegalArgumentException("Password hash không được rỗng");
    }
    this.passwordHash = passwordHash;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }
  public void setUsername(String username) {
    this.username = username;
  }
  public void setGender(String gender) {
    this.gender = gender;
  }
  public void setDateOfBirth(String dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }
  public void setActive(boolean active) {
    this.active = active;
  }
}
