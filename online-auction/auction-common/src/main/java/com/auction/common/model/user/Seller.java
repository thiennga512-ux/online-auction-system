package com.auction.common.model.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 * Class Seller — Người bán hàng (đăng sản phẩm để đấu giá)
 * ============================================================
 *
 * 🎓 GIẢI THÍCH:
 * Seller kế thừa User và thêm các đặc điểm riêng:
 *   - shopName    : tên cửa hàng/shop
 *   - rating      : điểm đánh giá từ người mua (0.0 - 5.0)
 *   - listedItems : danh sách ID sản phẩm đã đăng bán
 *   - balance     : số tiền thu được từ các phiên đấu giá thắng
 *
 * Seller có quyền:
 *   - Đăng sản phẩm lên đấu giá
 *   - Xem kết quả phiên đấu giá của mình
 *   - Huỷ phiên đấu giá chưa bắt đầu
 * ============================================================
 */
public class Seller extends Bidder {

  /** Tên cửa hàng/thương hiệu của seller */
  private String shopName;

  /** Số CCCD / CMND (định danh người bán) */
  private String citizenId;

  /** Điểm uy tín (0.0 - 5.0), tính trung bình từ các đánh giá */
  private double rating;

  /** Tổng số đánh giá đã nhận (để tính average chính xác) */
  private int ratingCount;

  /** Danh sách ID sản phẩm mà seller này đã đăng */
  private final List<String> listedItemIds;

  /**
   * Số dư tài khoản (VND).
   * Tăng khi phiên đấu giá kết thúc và Bidder thắng.
   * Giảm khi seller rút tiền.
   */
  private double balance;

  // -------------------------------------------------------
  // CONSTRUCTORS
  // -------------------------------------------------------

  /** Constructor tải từ database */
  public Seller(String id, String fullName, String username, String email, String passwordHash,
      String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt, boolean active,
      double depositBalance, double frozenBalance, String shippingAddress, int totalBidsPlaced,
      String shopName, String citizenId, double rating, int ratingCount, double balance) {
    super(id, fullName, username, email, passwordHash, phoneNumber, gender, dateOfBirth, createdAt, active, 
          depositBalance, frozenBalance, shippingAddress, totalBidsPlaced, UserRole.SELLER);
    this.shopName = shopName;
    this.citizenId = citizenId;
    this.rating = rating;
    this.ratingCount = ratingCount;
    this.balance = balance;
    this.listedItemIds = new ArrayList<>();
  }

  /** Constructor tạo mới Seller */
  public Seller(String fullName, String username, String email, String passwordHash, String gender, String dateOfBirth, String shopName, String citizenId) {
    super(fullName, username, email, passwordHash, gender, dateOfBirth, UserRole.SELLER);
    this.shopName = shopName;
    this.citizenId = citizenId;
    this.rating = 0.0;
    this.ratingCount = 0;
    this.balance = 0.0;
    this.listedItemIds = new ArrayList<>();
  }

  // -------------------------------------------------------
  // IMPLEMENT ABSTRACT METHODS
  // -------------------------------------------------------


  @Override
  public String getDashboardView() {
    return "seller-dashboard.fxml";
  }

  // -------------------------------------------------------
  // BUSINESS METHODS
  // -------------------------------------------------------

  /**
   * Ghi nhận sản phẩm mới được đăng bán.
   *
   * @param itemId ID sản phẩm mới đăng
   */
  public void addListedItem(String itemId) {
    if (itemId != null && !listedItemIds.contains(itemId)) {
      listedItemIds.add(itemId);
    }
  }

  /**
   * Cập nhật điểm rating sau khi nhận đánh giá mới.
   * Tính bằng công thức trung bình động (running average).
   *
   * Ví dụ: rating hiện tại = 4.0 (3 lần), nhận thêm đánh giá 5.0
   * → rating mới = (4.0 * 3 + 5.0) / 4 = 4.25
   *
   * @param newRating điểm đánh giá mới (1.0 - 5.0)
   */
  public void addRating(double newRating) {
    if (newRating < 1.0 || newRating > 5.0) {
      throw new IllegalArgumentException("Rating phải từ 1.0 đến 5.0, nhận: " + newRating);
    }
    // Công thức tính trung bình động
    this.rating = (this.rating * this.ratingCount + newRating) / (this.ratingCount + 1);
    this.ratingCount++;
  }

  /**
   * Cộng tiền vào số dư khi phiên đấu giá kết thúc thắng lợi.
   *
   * @param amount số tiền nhận được (đã trừ phí hoa hồng)
   */
  public void addEarnings(double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Số tiền phải dương: " + amount);
    }
    this.balance += amount;
  }

  /**
   * Kiểm tra xem Seller đã đủ uy tín để đăng sản phẩm cao cấp không.
   * (Business rule: cần rating >= 3.5 và ít nhất 5 lượt đánh giá)
   *
   * @return true nếu đủ điều kiện
   */
  public boolean isTrustedSeller() {
    return ratingCount >= 5 && rating >= 3.5;
  }

  // -------------------------------------------------------
  // GETTERS & SETTERS
  // -------------------------------------------------------

  public String getShopName() { return shopName; }
  public void setShopName(String shopName) { this.shopName = shopName; }

  public String getCitizenId() { return citizenId; }
  public void setCitizenId(String citizenId) { this.citizenId = citizenId; }

  public double getRating() { return rating; }
  public int getRatingCount() { return ratingCount; }

  public double getBalance() { return balance; }

  public List<String> getListedItemIds() {
    return Collections.unmodifiableList(listedItemIds);
  }

  @Override
  public String toString() {
    return String.format("[SELLER] %s | Shop: %s | Rating: %.1f (%d lượt)",
        getFullName(), shopName, rating, ratingCount);
  }
}
