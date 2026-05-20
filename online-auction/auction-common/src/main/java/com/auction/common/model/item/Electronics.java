package com.auction.common.model.item;

import java.time.LocalDateTime;

/**
 * ============================================================
 * Class Electronics — Sản phẩm Điện tử Công nghệ
 * ============================================================
 *
 * 🎓 GIẢI THÍCH KẾ THỪA:
 * Electronics kế thừa Item và thêm các thuộc tính đặc thù:
 *   - brand         : thương hiệu (Apple, Samsung, Dell...)
 *   - model         : mã model cụ thể
 *   - warrantyMonths: thời hạn bảo hành (tháng)
 *   - condition     : tình trạng máy (NEW, LIKE_NEW, USED, FOR_PARTS)
 *
 * Phí hoa hồng: 3% giá bán (lấy từ ItemCategory.ELECTRONICS)
 * ============================================================
 */
public class Electronics extends Item {

  /**
   * Enum lồng (nested enum) — chỉ dùng trong context Electronics
   * Mô tả tình trạng sản phẩm điện tử
   */
  public enum Condition {
    NEW("Mới 100%"),
    LIKE_NEW("Như mới (99%)"),
    USED("Đã qua sử dụng"),
    FOR_PARTS("Thanh lý phụ tùng");

    private final String displayName;

    Condition(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
  }

  // -------------------------------------------------------
  // FIELDS ĐẶC THÙ của Electronics
  // -------------------------------------------------------
  private String brand;           // Thương hiệu
  private String model;           // Mã model
  private int warrantyMonths;     // Thời hạn bảo hành (tháng, 0 = không bảo hành)
  private Condition condition;    // Tình trạng sản phẩm

  // -------------------------------------------------------
  // CONSTRUCTORS
  // -------------------------------------------------------

  /** Constructor tải từ database */
  public Electronics(String id, String name, String description, double basePrice,
      double minIncrement, String sellerId, String imageUrl, boolean available,
      LocalDateTime listedAt, String brand, String model,
      int warrantyMonths, Condition condition) {
    // Gọi super với category = ELECTRONICS
    super(id, name, description, basePrice, minIncrement, sellerId,
        ItemCategory.ELECTRONICS, imageUrl, available, listedAt);
    this.brand = brand;
    this.model = model;
    this.warrantyMonths = warrantyMonths;
    this.condition = condition;
  }

  /** Constructor tạo mới Electronics */
  public Electronics(String name, String description, double basePrice,
      double minIncrement, String sellerId, String imageUrl,
      String brand, String model, int warrantyMonths, Condition condition) {
    super(name, description, basePrice, minIncrement, sellerId,
        ItemCategory.ELECTRONICS, imageUrl);
    this.brand = brand;
    this.model = model;
    this.warrantyMonths = warrantyMonths;
    this.condition = condition;
  }

  // -------------------------------------------------------
  // IMPLEMENT ABSTRACT METHODS
  // -------------------------------------------------------

  /**
   * 🔑 ĐA HÌNH: Tính phí hoa hồng Electronics = 3% giá bán.
   * Tỷ lệ lấy từ enum ItemCategory để tránh magic number.
   */
  @Override
  public double getCategoryFee(double finalPrice) {
    return ItemCategory.ELECTRONICS.calculateFee(finalPrice);
    // Tương đương: return finalPrice * 0.03;
  }

  /**
   * 🔑 TRỪU TƯỢNG: Kiểm tra tính hợp lệ của sản phẩm Electronics.
   */
  @Override
  public void validate() {
    if (brand == null || brand.isBlank()) {
      throw new IllegalStateException("Electronics phải có thương hiệu (brand)");
    }
    if (model == null || model.isBlank()) {
      throw new IllegalStateException("Electronics phải có mã model");
    }
    if (warrantyMonths < 0) {
      throw new IllegalStateException("Thời hạn bảo hành không được âm: " + warrantyMonths);
    }
    if (condition == null) {
      throw new IllegalStateException("Electronics phải có thông tin tình trạng (condition)");
    }
  }

  /**
   * 🔑 ĐA HÌNH: Mô tả kỹ thuật của Electronics.
   * Hiển thị trên danh sách sản phẩm phía Client.
   */
  @Override
  public String getTechnicalSummary() {
    String warranty = warrantyMonths > 0
        ? warrantyMonths + " tháng bảo hành"
        : "Không bảo hành";
    return String.format("%s %s | %s | %s", brand, model, condition.getDisplayName(), warranty);
  }

  // -------------------------------------------------------
  // GETTERS & SETTERS
  // -------------------------------------------------------

  public String getBrand() { return brand; }
  public void setBrand(String brand) { this.brand = brand; }

  public String getModel() { return model; }
  public void setModel(String model) { this.model = model; }

  public int getWarrantyMonths() { return warrantyMonths; }
  public void setWarrantyMonths(int warrantyMonths) {
    if (warrantyMonths < 0) throw new IllegalArgumentException("Bảo hành không được âm");
    this.warrantyMonths = warrantyMonths;
  }

  public Condition getCondition() { return condition; }
  public void setCondition(Condition condition) { this.condition = condition; }

  @Override
  public String toString() {
    return String.format("[ELECTRONICS] %s %s - %s | Giá: %.0f VND",
        brand, model, condition.getDisplayName(), getBasePrice());
  }
}
