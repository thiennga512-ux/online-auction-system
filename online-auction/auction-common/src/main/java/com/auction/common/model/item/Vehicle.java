package com.auction.common.model.item;

import java.time.LocalDateTime;

/**
 * ============================================================
 * Class Vehicle — Xe cộ & Phương tiện giao thông
 * ============================================================
 *
 * 🎓 GIẢI THÍCH:
 * Vehicle là loại sản phẩm có giá trị lớn nhất trong hệ thống.
 * Thuộc tính kỹ thuật đặc thù bao gồm:
 *   - make/model/year  : hãng xe, model, năm sản xuất
 *   - mileage          : số km đã đi
 *   - fuelType         : loại nhiên liệu
 *   - transmission     : hộp số (số sàn / số tự động)
 *   - licensePlate     : biển số xe
 *   - hasValidRegistry : có giấy đăng ký hợp lệ không
 *
 * Phí hoa hồng thấp nhất: 2% (xe có giá lớn, phí % thấp là hợp lý)
 * ============================================================
 */
public class Vehicle extends Item {

  // -------------------------------------------------------
  // ENUMS LỒNG (Nested Enums)
  // -------------------------------------------------------

  /** Loại nhiên liệu */
  public enum FuelType {
    GASOLINE("Xăng"),
    DIESEL("Dầu diesel"),
    ELECTRIC("Điện"),
    HYBRID("Hybrid"),
    OTHER("Khác");

    private final String displayName;
    FuelType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
  }

  /** Loại hộp số */
  public enum Transmission {
    MANUAL("Số sàn"),
    AUTOMATIC("Số tự động"),
    CVT("CVT"),
    SEMI_AUTO("Bán tự động");

    private final String displayName;
    Transmission(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
  }

  /** Loại phương tiện */
  public enum VehicleType {
    CAR("Ô tô"),
    MOTORCYCLE("Xe máy"),
    TRUCK("Xe tải"),
    BOAT("Thuyền/Ca nô"),
    OTHER("Phương tiện khác");

    private final String displayName;
    VehicleType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
  }

  // -------------------------------------------------------
  // FIELDS ĐẶC THÙ của Vehicle
  // -------------------------------------------------------

  private VehicleType vehicleType;  // Loại phương tiện
  private String make;              // Hãng sản xuất (Toyota, Honda, BMW...)
  private String model;             // Model xe (Camry, Civic, X5...)
  private int year;                 // Năm sản xuất
  private double mileage;           // Số km đã chạy (−1 nếu là xe mới)
  private FuelType fuelType;        // Loại nhiên liệu
  private Transmission transmission;// Loại hộp số
  private String color;             // Màu sắc
  private String licensePlate;      // Biển số xe (null = chưa đăng ký)
  private boolean hasValidRegistry; // Giấy đăng ký xe còn hạn

  // -------------------------------------------------------
  // CONSTRUCTORS
  // -------------------------------------------------------

  /** Constructor tải từ database */
  public Vehicle(String id, String name, String description, double basePrice,
      double minIncrement, String sellerId, String imageUrl, boolean available,
      LocalDateTime listedAt, VehicleType vehicleType, String make, String model,
      int year, double mileage, FuelType fuelType, Transmission transmission,
      String color, String licensePlate, boolean hasValidRegistry) {
    super(id, name, description, basePrice, minIncrement, sellerId,
        ItemCategory.VEHICLE, imageUrl, available, listedAt);
    this.vehicleType = vehicleType;
    this.make = make;
    this.model = model;
    this.year = year;
    this.mileage = mileage;
    this.fuelType = fuelType;
    this.transmission = transmission;
    this.color = color;
    this.licensePlate = licensePlate;
    this.hasValidRegistry = hasValidRegistry;
  }

  /** Constructor tạo mới Vehicle */
  public Vehicle(String name, String description, double basePrice,
      double minIncrement, String sellerId, String imageUrl,
      VehicleType vehicleType, String make, String model,
      int year, double mileage, FuelType fuelType, Transmission transmission,
      String color, String licensePlate, boolean hasValidRegistry) {
    super(name, description, basePrice, minIncrement, sellerId,
        ItemCategory.VEHICLE, imageUrl);
    this.vehicleType = vehicleType;
    this.make = make;
    this.model = model;
    this.year = year;
    this.mileage = mileage;
    this.fuelType = fuelType;
    this.transmission = transmission;
    this.color = color;
    this.licensePlate = licensePlate;
    this.hasValidRegistry = hasValidRegistry;
  }

  // -------------------------------------------------------
  // IMPLEMENT ABSTRACT METHODS
  // -------------------------------------------------------

  /**
   * 🔑 ĐA HÌNH: Phí hoa hồng Vehicle = 2%.
   * Thấp nhất vì giá trị xe lớn → 2% đã đủ lớn.
   */
  @Override
  public double getCategoryFee(double finalPrice) {
    return ItemCategory.VEHICLE.calculateFee(finalPrice);
    // Tương đương: return finalPrice * 0.02;
  }

  /**
   * 🔑 TRỪU TƯỢNG: Kiểm tra tính hợp lệ của phương tiện.
   */
  @Override
  public void validate() {
    if (make == null || make.isBlank()) {
      throw new IllegalStateException("Phải ghi rõ hãng sản xuất (make)");
    }
    if (model == null || model.isBlank()) {
      throw new IllegalStateException("Phải ghi rõ model xe");
    }
    int currentYear = LocalDateTime.now().getYear();
    if (year < 1886 || year > currentYear + 1) {
      // 1886: năm xe hơi đầu tiên được phát minh
      throw new IllegalStateException("Năm sản xuất không hợp lệ: " + year);
    }
    if (mileage < 0 && mileage != -1) {
      // -1 là giá trị đặc biệt cho xe mới (chưa lăn bánh)
      throw new IllegalStateException("Số km không được âm (dùng -1 cho xe mới)");
    }
    if (vehicleType == null) {
      throw new IllegalStateException("Phải xác định loại phương tiện");
    }
  }

  /**
   * 🔑 ĐA HÌNH: Mô tả kỹ thuật của Vehicle.
   */
  @Override
  public String getTechnicalSummary() {
    String mileageStr = mileage < 0 ? "Xe mới" : String.format("%.0f km", mileage);
    String regStr = hasValidRegistry ? "✅ Đăng ký hợp lệ" : "⚠️ Chưa đăng ký";
    return String.format("%s %s %d | %s | %s | %s | %s",
        make, model, year,
        fuelType.getDisplayName(),
        mileageStr,
        transmission.getDisplayName(),
        regStr);
  }

  /**
   * Tính tuổi xe (số năm từ khi sản xuất đến hiện tại).
   *
   * @return số năm tuổi của xe
   */
  public int getAge() {
    return LocalDateTime.now().getYear() - year;
  }

  // -------------------------------------------------------
  // GETTERS & SETTERS
  // -------------------------------------------------------

  public VehicleType getVehicleType() { return vehicleType; }
  public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

  public String getMake() { return make; }
  public void setMake(String make) { this.make = make; }

  public String getModel() { return model; }
  public void setModel(String model) { this.model = model; }

  public int getYear() { return year; }
  public void setYear(int year) { this.year = year; }

  public double getMileage() { return mileage; }
  public void setMileage(double mileage) { this.mileage = mileage; }

  public FuelType getFuelType() { return fuelType; }
  public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }

  public Transmission getTransmission() { return transmission; }
  public void setTransmission(Transmission transmission) { this.transmission = transmission; }

  public String getColor() { return color; }
  public void setColor(String color) { this.color = color; }

  public String getLicensePlate() { return licensePlate; }
  public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

  public boolean isHasValidRegistry() { return hasValidRegistry; }
  public void setHasValidRegistry(boolean hasValidRegistry) {
    this.hasValidRegistry = hasValidRegistry;
  }

  @Override
  public String toString() {
    return String.format("[VEHICLE] %s %s %d | %.0f km | %s | Giá: %.0f VND",
        make, model, year, mileage, color, getBasePrice());
  }
}
