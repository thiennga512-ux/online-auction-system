package com.auction.common.model.user;

/**
 * ============================================================
 * Enum UserRole — Vai trò của người dùng trong hệ thống
 * ============================================================
 *
 * 🎓 GIẢI THÍCH CHO NGƯỜI MỚI:
 * "Enum" (Enumeration) là kiểu dữ liệu đặc biệt dùng để định nghĩa
 * một tập hợp CỐ ĐỊNH các hằng số có tên.
 *
 * Tại sao dùng Enum thay vì String?
 *   ❌ Dùng String: user.setRole("ADMIN") — dễ gõ sai thành "admin", "Admin"...
 *   ✅ Dùng Enum:   user.setRole(UserRole.ADMIN) — IDE tự gợi ý, không gõ sai được
 *
 * Mỗi giá trị trong Enum có thêm 2 thuộc tính:
 *   - displayName: tên hiển thị thân thiện với người dùng
 *   - level: cấp độ quyền hạn (Admin = 3 quyền nhất)
 * ============================================================
 */
public enum UserRole {

  // Cú pháp: TÊN_HẰNG("displayName", level)
  ADMIN("Quản trị viên", 3),
  SELLER("Người bán", 2),
  BIDDER("Người đặt giá", 1);

  // -------------------------------------------------------
  // Mỗi Enum constant có thể mang thêm dữ liệu riêng
  // -------------------------------------------------------
  private final String displayName;  // Tên hiện thị tiếng Việt
  private final int level;           // Cấp độ quyền hạn

  /**
   * Constructor của Enum — được gọi khi JVM khởi tạo ADMIN, SELLER, BIDDER.
   * Constructor của Enum LUÔN là private (quy tắc bắt buộc).
   */
  UserRole(String displayName, int level) {
    this.displayName = displayName;
    this.level = level;
  }

  /** @return Tên hiển thị tiếng Việt, ví dụ: "Quản trị viên" */
  public String getDisplayName() {
    return displayName;
  }

  /** @return Cấp độ quyền hạn (1-3), dùng để kiểm tra authorization */
  public int getLevel() {
    return level;
  }

  /**
   * Chuyển đổi String → UserRole (không phân biệt hoa/thường).
   * Ví dụ: UserRole.fromString("admin") → UserRole.ADMIN
   *
   * @param value chuỗi đầu vào từ JSON hoặc user nhập
   * @return UserRole tương ứng
   * @throws IllegalArgumentException nếu không tìm thấy role phù hợp
   */
  public static UserRole fromString(String value) {
    if (value == null) {
      throw new IllegalArgumentException("UserRole không được null");
    }
    // Chuyển về UPPER_CASE rồi tìm trong các Enum constant
    for (UserRole role : values()) {
      if (role.name().equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Không tìm thấy UserRole: " + value);
  }

  @Override
  public String toString() {
    return displayName;
  }
}
