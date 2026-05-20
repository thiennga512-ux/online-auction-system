package com.auction.common.model.item;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ============================================================
 * Abstract Class Item — Lớp CHA trừu tượng cho mọi loại sản phẩm
 * ============================================================
 *
 * 🎓 GIẢI THÍCH THIẾT KẾ:
 *
 * Tại sao Item là abstract?
 * → Trong hệ thống đấu giá, không tồn tại một "Item chung chung".
 *   Mỗi sản phẩm đều phải thuộc một danh mục: Electronics, Art, hoặc Vehicle.
 *   → Abstract class buộc developer phải chọn đúng loại.
 *
 * Abstract method getCategoryFee(double finalPrice):
 * → Mỗi loại sản phẩm có công thức tính phí hoa hồng khác nhau
 *   (xem ItemCategory enum để hiểu tỷ lệ phí).
 * → Đây là minh chứng rõ ràng nhất cho TRỪU TƯỢNG + ĐA HÌNH trong project.
 *
 * Abstract method validate():
 * → Mỗi loại sản phẩm có quy tắc hợp lệ khác nhau.
 *   Electronics cần kiểm tra warrantyMonths >= 0.
 *   Vehicle cần kiểm tra mileage >= 0, year hợp lệ...
 *
 * ============================================================
 */
public abstract class Item {

  // -------------------------------------------------------
  // COMMON FIELDS — Thuộc tính chung cho MỌI loại sản phẩm
  // -------------------------------------------------------

  /** ID duy nhất của sản phẩm */
  private final String id;

  /** Tên sản phẩm */
  private String name;

  /** Mô tả chi tiết */
  private String description;

  /**
   * Giá khởi điểm (base price / starting price).
   * Đây là mức giá tối thiểu để bắt đầu đấu giá.
   */
  private double basePrice;

  /**
   * Bước giá tối thiểu (minimum increment).
   * Mỗi bid mới phải cao hơn bid trước ít nhất bằng giá trị này.
   * Ví dụ: currentPrice = 1.000.000, increment = 50.000
   *   → Bid tiếp theo phải >= 1.050.000
   */
  private double minIncrement;

  /** ID của Seller đã đăng sản phẩm này */
  private String sellerId;

  /** Danh mục sản phẩm (là Enum, không phải String) */
  private final ItemCategory category;

  /** URL ảnh sản phẩm (lưu đường dẫn, ảnh thật ở Giai đoạn 4) */
  private String imageUrl;

  /** Tình trạng: true = còn hàng, false = đã bán */
  private boolean available;

  /** Thời điểm đăng sản phẩm */
  private final LocalDateTime listedAt;

  // -------------------------------------------------------
  // CONSTRUCTORS
  // -------------------------------------------------------

  /**
   * Constructor tải từ database (có sẵn ID).
   * "protected" vì chỉ subclass và cùng package mới được gọi.
   */
  protected Item(String id, String name, String description,
      double basePrice, double minIncrement, String sellerId,
      ItemCategory category, String imageUrl, boolean available,
      LocalDateTime listedAt) {
    // Validation cơ bản ngay trong constructor
    validateBaseData(name, basePrice, minIncrement);

    this.id = id;
    this.name = name;
    this.description = description;
    this.basePrice = basePrice;
    this.minIncrement = minIncrement;
    this.sellerId = sellerId;
    this.category = category;
    this.imageUrl = imageUrl;
    this.available = available;
    this.listedAt = listedAt;
  }

  /**
   * Constructor tạo mới Item (tự sinh ID và thời gian).
   */
  protected Item(String name, String description, double basePrice,
      double minIncrement, String sellerId, ItemCategory category, String imageUrl) {
    this(
        UUID.randomUUID().toString(),
        name, description, basePrice, minIncrement,
        sellerId, category, imageUrl, true, LocalDateTime.now()
    );
  }

  // -------------------------------------------------------
  // ABSTRACT METHODS — Subclass BẮT BUỘC implement
  // -------------------------------------------------------

  /**
   * 🔑 TRỪU TƯỢNG + ĐA HÌNH:
   * Tính phí hoa hồng của platform dựa trên giá bán cuối.
   *
   * Electronics: 3% × finalPrice
   * Art        : 5% × finalPrice
   * Vehicle    : 2% × finalPrice
   *
   * Gọi user.item.getCategoryFee(finalPrice) → tự động chọn đúng loại
   *
   * @param finalPrice giá bán thực tế (giá bid cao nhất)
   * @return số tiền hoa hồng phải trả cho platform
   */
  public abstract double getCategoryFee(double finalPrice);

  /**
   * 🔑 TRỪU TƯỢNG:
   * Mỗi loại Item có quy tắc hợp lệ khác nhau.
   * Subclass tự kiểm tra các field đặc thù của mình.
   *
   * @throws IllegalStateException nếu dữ liệu không hợp lệ
   */
  public abstract void validate();

  /**
   * 🔑 ĐA HÌNH — Trả về mô tả kỹ thuật ngắn gọn.
   * Mỗi loại Item sẽ trả về thông tin khác nhau:
   *   Electronics: "Laptop | Bảo hành: 12 tháng"
   *   Vehicle    : "Toyota Camry 2020 | 15.000 km"
   *   Art        : "Tranh sơn dầu | Họa sĩ: Nguyễn Văn A"
   *
   * @return chuỗi mô tả kỹ thuật
   */
  public abstract String getTechnicalSummary();

  // -------------------------------------------------------
  // PRIVATE HELPER METHOD
  // -------------------------------------------------------

  /**
   * Kiểm tra dữ liệu cơ bản — dùng trong constructor.
   * Tách ra method riêng để tránh lặp lại code trong cả 2 constructor.
   */
  private void validateBaseData(String name, double basePrice, double minIncrement) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Tên sản phẩm không được để trống");
    }
    if (basePrice <= 0) {
      throw new IllegalArgumentException("Giá khởi điểm phải > 0, nhận: " + basePrice);
    }
    if (minIncrement <= 0) {
      throw new IllegalArgumentException("Bước giá tối thiểu phải > 0, nhận: " + minIncrement);
    }
  }

  // -------------------------------------------------------
  // CONCRETE METHODS — Logic dùng chung
  // -------------------------------------------------------

  /**
   * Kiểm tra xem giá bid có hợp lệ không (đủ bước giá).
   *
   * @param currentPrice   giá hiện tại của phiên đấu giá
   * @param proposedBid    giá muốn đặt
   * @return true nếu giá đề xuất hợp lệ
   */
  public boolean isBidValid(double currentPrice, double proposedBid) {
    return proposedBid >= currentPrice + minIncrement;
  }

  /**
   * Tính giá bid tối thiểu tiếp theo từ giá hiện tại.
   *
   * @param currentPrice giá hiện tại
   * @return giá tối thiểu của bid tiếp theo
   */
  public double getNextMinimumBid(double currentPrice) {
    return currentPrice + minIncrement;
  }

  @Override
  public String toString() {
    return String.format("[%s] %s | Giá khởi điểm: %.0f VND | Bước giá: %.0f VND",
        category.name(), name, basePrice, minIncrement);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Item other)) return false;
    return this.id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  // -------------------------------------------------------
  // GETTERS & SETTERS
  // -------------------------------------------------------

  public String getId() { return id; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public double getBasePrice() { return basePrice; }
  public double getMinIncrement() { return minIncrement; }
  public String getSellerId() { return sellerId; }
  public ItemCategory getCategory() { return category; }
  public String getImageUrl() { return imageUrl; }
  public boolean isAvailable() { return available; }
  public LocalDateTime getListedAt() { return listedAt; }

  public void setName(String name) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Tên không được trống");
    this.name = name;
  }

  public void setDescription(String description) { this.description = description; }

  public void setBasePrice(double basePrice) {
    if (basePrice <= 0) throw new IllegalArgumentException("Giá phải > 0");
    this.basePrice = basePrice;
  }

  public void setMinIncrement(double minIncrement) {
    if (minIncrement <= 0) throw new IllegalArgumentException("Bước giá phải > 0");
    this.minIncrement = minIncrement;
  }

  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  public void setAvailable(boolean available) { this.available = available; }
  public void setSellerId(String sellerId) { this.sellerId = sellerId; }
}
