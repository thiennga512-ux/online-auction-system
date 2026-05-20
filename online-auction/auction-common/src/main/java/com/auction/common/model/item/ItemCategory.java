package com.auction.common.model.item;

/**
 * ============================================================
 * Enum ItemCategory — Danh mục sản phẩm đấu giá
 * ============================================================
 *
 * 🎓 GIẢI THÍCH:
 * Mỗi danh mục sản phẩm có mức phí dịch vụ (commissionRate) khác nhau.
 * Đây là lý do tại sao Item có abstract method getCategoryFee():
 *   - Electronics → 3% giá bán
 *   - Art         → 5% (tác phẩm nghệ thuật thường có giá cao)
 *   - Vehicle     → 2% (xe cộ thường có giá trị lớn, phí thấp hơn)
 *
 * Lưu trữ logic phí này trong Enum giúp các class Item không cần
 * hardcode con số, dễ thay đổi sau này.
 * ============================================================
 */
public enum ItemCategory {

  ELECTRONICS("Điện tử - Công nghệ", 0.03, "Laptop, điện thoại, thiết bị..."),
  ART("Nghệ thuật - Cổ vật", 0.05, "Tranh, tượng, đồ cổ..."),
  VEHICLE("Xe cộ - Phương tiện", 0.02, "Ô tô, xe máy, thuyền...");

  private final String displayName;    // Tên hiển thị
  private final double commissionRate; // Tỷ lệ hoa hồng (0.03 = 3%)
  private final String description;   // Mô tả ngắn

  ItemCategory(String displayName, double commissionRate, String description) {
    this.displayName = displayName;
    this.commissionRate = commissionRate;
    this.description = description;
  }

  public String getDisplayName() { return displayName; }
  public double getCommissionRate() { return commissionRate; }
  public String getDescription() { return description; }

  /**
   * Tính phí hoa hồng dựa trên giá bán thực tế.
   * Ví dụ: ELECTRONICS.calculateFee(10_000_000) → 300_000 đồng
   *
   * @param finalPrice giá bán cuối cùng (đồng)
   * @return số tiền hoa hồng phải trả
   */
  public double calculateFee(double finalPrice) {
    return finalPrice * commissionRate;
  }

  /** Chuyển String → ItemCategory, không phân biệt hoa/thường */
  public static ItemCategory fromString(String value) {
    if (value == null) {
      throw new IllegalArgumentException("ItemCategory không được null");
    }
    for (ItemCategory cat : values()) {
      if (cat.name().equalsIgnoreCase(value)) {
        return cat;
      }
    }
    throw new IllegalArgumentException("Không tìm thấy ItemCategory: " + value);
  }

  @Override
  public String toString() {
    return displayName;
  }
}
