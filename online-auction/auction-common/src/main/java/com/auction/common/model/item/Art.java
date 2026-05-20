package com.auction.common.model.item;

import java.time.LocalDateTime;

/**
 * ============================================================
 * Class Art — Tác phẩm Nghệ thuật & Cổ vật
 * ============================================================
 *
 * 🎓 GIẢI THÍCH:
 * Art là loại sản phẩm có giá trị cao và chủ quan nhất.
 * Các thuộc tính đặc thù:
 *   - artistName  : tên nghệ sĩ/tác giả
 *   - creationYear: năm sáng tác
 *   - medium      : chất liệu (sơn dầu, màu nước, đồng...)
 *   - isAuthenticated: đã được kiểm định thật/giả chưa
 *   - certificateId  : mã chứng chỉ xác thực (nếu có)
 *
 * Phí hoa hồng cao nhất: 5% (vì giá trị lớn, tính phí nhiều hơn)
 * ============================================================
 */
public class Art extends Item {

  /** Tên nghệ sĩ/tác giả tạo ra tác phẩm */
  private String artistName;

  /** Năm sáng tác (0 nếu không rõ) */
  private int creationYear;

  /** Chất liệu sáng tác: "Sơn dầu", "Màu nước", "Đồng", "Gỗ"... */
  private String medium;

  /**
   * Đã được kiểm định bởi chuyên gia chưa?
   * true = có giấy chứng nhận thật
   * false = chưa kiểm định (rủi ro cao hơn cho Bidder)
   */
  private boolean authenticated;

  /** Mã số chứng chỉ kiểm định (null nếu chưa kiểm định) */
  private String certificateId;

  /** Kích thước hoặc thông số vật lý: "80x120 cm", "45kg"... */
  private String dimensions;

  // -------------------------------------------------------
  // CONSTRUCTORS
  // -------------------------------------------------------

  /** Constructor tải từ database */
  public Art(String id, String name, String description, double basePrice,
      double minIncrement, String sellerId, String imageUrl, boolean available,
      LocalDateTime listedAt, String artistName, int creationYear,
      String medium, boolean authenticated, String certificateId, String dimensions) {
    super(id, name, description, basePrice, minIncrement, sellerId,
        ItemCategory.ART, imageUrl, available, listedAt);
    this.artistName = artistName;
    this.creationYear = creationYear;
    this.medium = medium;
    this.authenticated = authenticated;
    this.certificateId = certificateId;
    this.dimensions = dimensions;
  }

  /** Constructor tạo mới Art */
  public Art(String name, String description, double basePrice,
      double minIncrement, String sellerId, String imageUrl,
      String artistName, int creationYear, String medium,
      boolean authenticated, String certificateId, String dimensions) {
    super(name, description, basePrice, minIncrement, sellerId,
        ItemCategory.ART, imageUrl);
    this.artistName = artistName;
    this.creationYear = creationYear;
    this.medium = medium;
    this.authenticated = authenticated;
    this.certificateId = certificateId;
    this.dimensions = dimensions;
  }

  // -------------------------------------------------------
  // IMPLEMENT ABSTRACT METHODS
  // -------------------------------------------------------

  /**
   * 🔑 ĐA HÌNH: Phí hoa hồng Art = 5% giá bán.
   * Cao hơn Electronics (3%) vì giá trị trung bình cao hơn.
   */
  @Override
  public double getCategoryFee(double finalPrice) {
    return ItemCategory.ART.calculateFee(finalPrice);
    // Tương đương: return finalPrice * 0.05;
  }

  /**
   * 🔑 TRỪU TƯỢNG: Kiểm tra tính hợp lệ của tác phẩm nghệ thuật.
   */
  @Override
  public void validate() {
    if (artistName == null || artistName.isBlank()) {
      throw new IllegalStateException("Tác phẩm nghệ thuật phải có tên nghệ sĩ");
    }
    if (medium == null || medium.isBlank()) {
      throw new IllegalStateException("Phải ghi rõ chất liệu (medium)");
    }
    // Nếu đã có certificate thì phải có ID chứng chỉ
    if (authenticated && (certificateId == null || certificateId.isBlank())) {
      throw new IllegalStateException(
          "Tác phẩm tuyên bố đã kiểm định nhưng thiếu mã chứng chỉ");
    }
    if (creationYear < 0) {
      throw new IllegalStateException("Năm sáng tác không hợp lệ: " + creationYear);
    }
  }

  /**
   * 🔑 ĐA HÌNH: Mô tả tác phẩm nghệ thuật.
   */
  @Override
  public String getTechnicalSummary() {
    String yearStr = creationYear > 0 ? "Năm " + creationYear : "Năm không rõ";
    String authStr = authenticated ? "✅ Đã kiểm định" : "⚠️ Chưa kiểm định";
    return String.format("%s | %s | %s | %s", artistName, medium, yearStr, authStr);
  }

  // -------------------------------------------------------
  // GETTERS & SETTERS
  // -------------------------------------------------------

  public String getArtistName() { return artistName; }
  public void setArtistName(String artistName) { this.artistName = artistName; }

  public int getCreationYear() { return creationYear; }
  public void setCreationYear(int creationYear) { this.creationYear = creationYear; }

  public String getMedium() { return medium; }
  public void setMedium(String medium) { this.medium = medium; }

  public boolean isAuthenticated() { return authenticated; }
  public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

  public String getCertificateId() { return certificateId; }
  public void setCertificateId(String certificateId) { this.certificateId = certificateId; }

  public String getDimensions() { return dimensions; }
  public void setDimensions(String dimensions) { this.dimensions = dimensions; }

  @Override
  public String toString() {
    return String.format("[ART] %s - %s (%d) | %s | Giá: %.0f VND",
        getName(), artistName, creationYear, medium, getBasePrice());
  }
}
