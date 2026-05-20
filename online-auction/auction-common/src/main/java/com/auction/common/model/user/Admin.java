package com.auction.common.model.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 * Class Admin — Quản trị viên hệ thống
 * ============================================================
 *
 * 🎓 GIẢI THÍCH KẾ THỪA:
 * "extends User" có nghĩa là Admin KẾ THỪA toàn bộ từ User:
 *   ✅ Tự động có: id, fullName, email, passwordHash, isActive()...
 *   ✅ Tự động có: checkPassword(), hasPermission(), toString()...
 *   ❗ BẮT BUỘC implement: getRole(), getDashboardView() (vì là abstract)
 *
 * Admin có thêm các đặc quyền riêng:
 *   - Duyệt/từ chối phiên đấu giá
 *   - Quản lý tài khoản người dùng
 *   - Xem toàn bộ log giao dịch
 *   - adminNotes: ghi chú nội bộ
 * ============================================================
 */
public class Admin extends User {

  /** Ghi chú nội bộ của admin (hiển thị trên dashboard admin) */
  private String adminNotes;

  /**
   * Danh sách ID các phiên đấu giá đã được admin này duyệt.
   * Dùng để tracking trách nhiệm.
   */
  private final List<String> approvedAuctionIds;

  /** Constructor cho GSON deserialize */
  private Admin() {
    super(null, null, null, null, null, null, null, null, null, false, UserRole.ADMIN);
    this.approvedAuctionIds = new ArrayList<>();
  }

  // -------------------------------------------------------
  // CONSTRUCTORS
  // -------------------------------------------------------

  /**
   * Constructor tải từ database (có sẵn ID).
   * Chú ý: gọi super(...) để khởi tạo phần User trước.
   * "super" nghĩa là "gọi constructor của class cha".
   */
  public Admin(String id, String fullName, String username, String email, String passwordHash,
      String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt, boolean active, String adminNotes) {
    // Gọi constructor của class cha User
    super(id, fullName, username, email, passwordHash, phoneNumber, gender, dateOfBirth, createdAt, active, UserRole.ADMIN);
    this.adminNotes = adminNotes;
    this.approvedAuctionIds = new ArrayList<>();
  }

  /**
   * Constructor tạo mới Admin.
   */
  public Admin(String fullName, String username, String email, String passwordHash, String gender, String dateOfBirth) {
    super(fullName, username, email, passwordHash, gender, dateOfBirth, UserRole.ADMIN);
    this.adminNotes = "";
    this.approvedAuctionIds = new ArrayList<>();
  }

  // -------------------------------------------------------
  // IMPLEMENT ABSTRACT METHODS — BẮT BUỘC phải viết
  // -------------------------------------------------------


  /**
   * 🔑 ĐA HÌNH: Sau khi đăng nhập, Admin sẽ được điều hướng đến màn hình admin.
   */
  @Override
  public String getDashboardView() {
    return "admin-dashboard.fxml";
  }

  // -------------------------------------------------------
  // BUSINESS METHODS — Hành vi đặc trưng của Admin
  // -------------------------------------------------------

  /**
   * Ghi nhận rằng admin này đã duyệt một phiên đấu giá.
   * Giai đoạn 2 sẽ gọi method này trong AuctionService.
   *
   * @param auctionId ID phiên đấu giá được duyệt
   */
  public void approveAuction(String auctionId) {
    if (auctionId != null && !approvedAuctionIds.contains(auctionId)) {
      approvedAuctionIds.add(auctionId);
    }
  }

  /** @return Danh sách ID phiên đấu giá đã duyệt (chỉ đọc, không sửa được) */
  public List<String> getApprovedAuctionIds() {
    // Collections.unmodifiableList bảo vệ danh sách nội bộ khỏi bị sửa từ ngoài
    return Collections.unmodifiableList(approvedAuctionIds);
  }

  public String getAdminNotes() { return adminNotes; }
  public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
