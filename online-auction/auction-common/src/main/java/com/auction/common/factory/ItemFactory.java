package com.auction.common.factory;

import com.auction.common.model.item.Art;
import com.auction.common.model.item.Electronics;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemCategory;
import com.auction.common.model.item.Vehicle;
import java.time.LocalDateTime;

/**
 * ============================================================
 * Class ItemFactory — Factory Pattern tạo đối tượng Item
 * ============================================================
 *
 * 🎓 GIẢI THÍCH:
 * Tương tự UserFactory nhưng dành cho các loại Item.
 * Vấn đề đặc biệt của Item: mỗi subclass có rất nhiều field khác nhau.
 *
 * Giải pháp: sử dụng Builder Pattern kết hợp trong từng subclass,
 * và Factory quyết định loại nào được tạo.
 *
 * Thực tế khi Seller đăng sản phẩm mới:
 *   1. Client gửi JSON: { "category": "ELECTRONICS", "brand": "Apple", ... }
 *   2. Server nhận JSON, parse ra
 *   3. Gọi ItemFactory.create(category, ...) → nhận đúng loại Item
 *   4. Lưu vào SQLite qua ItemDAO
 * ============================================================
 */
public class ItemFactory {

  private ItemFactory() {
    throw new UnsupportedOperationException("ItemFactory là Utility Class");
  }

  // -------------------------------------------------------
  // FACTORY METHODS — Tạo Item mới (khi Seller đăng sản phẩm)
  // -------------------------------------------------------

  /**
   * Tạo Electronics mới.
   *
   * @param name         tên sản phẩm
   * @param description  mô tả
   * @param basePrice    giá khởi điểm
   * @param minIncrement bước giá tối thiểu
   * @param sellerId     ID người bán
   * @param imageUrl     URL ảnh
   * @param brand        thương hiệu
   * @param model        mã model
   * @param warrantyMonths tháng bảo hành
   * @param conditionStr  tình trạng: "NEW", "LIKE_NEW", "USED", "FOR_PARTS"
   * @return Electronics object mới
   */
  public static Electronics createElectronics(String name, String description,
      double basePrice, double minIncrement, String sellerId, String imageUrl,
      String brand, String model, int warrantyMonths, String conditionStr) {
    Electronics.Condition condition = Electronics.Condition.valueOf(conditionStr.toUpperCase());
    return new Electronics(name, description, basePrice, minIncrement,
        sellerId, imageUrl, brand, model, warrantyMonths, condition);
  }

  /**
   * Tạo Art (tác phẩm nghệ thuật) mới.
   */
  public static Art createArt(String name, String description,
      double basePrice, double minIncrement, String sellerId, String imageUrl,
      String artistName, int creationYear, String medium,
      boolean authenticated, String certificateId, String dimensions) {
    return new Art(name, description, basePrice, minIncrement,
        sellerId, imageUrl, artistName, creationYear, medium,
        authenticated, certificateId, dimensions);
  }

  /**
   * Tạo Vehicle mới.
   */
  public static Vehicle createVehicle(String name, String description,
      double basePrice, double minIncrement, String sellerId, String imageUrl,
      String vehicleTypeStr, String make, String model, int year,
      double mileage, String fuelTypeStr, String transmissionStr,
      String color, String licensePlate, boolean hasValidRegistry) {
    Vehicle.VehicleType vehicleType = Vehicle.VehicleType.valueOf(vehicleTypeStr.toUpperCase());
    Vehicle.FuelType fuelType = Vehicle.FuelType.valueOf(fuelTypeStr.toUpperCase());
    Vehicle.Transmission transmission = Vehicle.Transmission.valueOf(transmissionStr.toUpperCase());
    return new Vehicle(name, description, basePrice, minIncrement, sellerId, imageUrl,
        vehicleType, make, model, year, mileage, fuelType, transmission,
        color, licensePlate, hasValidRegistry);
  }

  // -------------------------------------------------------
  // FACTORY METHODS — Reconstruct từ Database
  // -------------------------------------------------------

  /**
   * Tạo lại Electronics từ dữ liệu SQLite.
   * ItemDAO sẽ gọi method này khi đọc từ bảng electronics_details.
   */
  public static Electronics reconstructElectronics(
      String id, String name, String description, double basePrice,
      double minIncrement, String sellerId, String imageUrl,
      boolean available, LocalDateTime listedAt,
      String brand, String model, int warrantyMonths, String conditionStr) {
    Electronics.Condition condition = Electronics.Condition.valueOf(conditionStr.toUpperCase());
    return new Electronics(id, name, description, basePrice, minIncrement,
        sellerId, imageUrl, available, listedAt, brand, model, warrantyMonths, condition);
  }

  /**
   * Tạo lại Art từ dữ liệu SQLite.
   */
  public static Art reconstructArt(
      String id, String name, String description, double basePrice,
      double minIncrement, String sellerId, String imageUrl,
      boolean available, LocalDateTime listedAt,
      String artistName, int creationYear, String medium,
      boolean authenticated, String certificateId, String dimensions) {
    return new Art(id, name, description, basePrice, minIncrement,
        sellerId, imageUrl, available, listedAt,
        artistName, creationYear, medium, authenticated, certificateId, dimensions);
  }

  /**
   * Tạo lại Vehicle từ dữ liệu SQLite.
   */
  public static Vehicle reconstructVehicle(
      String id, String name, String description, double basePrice,
      double minIncrement, String sellerId, String imageUrl,
      boolean available, LocalDateTime listedAt,
      String vehicleTypeStr, String make, String model, int year,
      double mileage, String fuelTypeStr, String transmissionStr,
      String color, String licensePlate, boolean hasValidRegistry) {
    Vehicle.VehicleType vehicleType = Vehicle.VehicleType.valueOf(vehicleTypeStr.toUpperCase());
    Vehicle.FuelType fuelType = Vehicle.FuelType.valueOf(fuelTypeStr.toUpperCase());
    Vehicle.Transmission transmission = Vehicle.Transmission.valueOf(transmissionStr.toUpperCase());
    return new Vehicle(id, name, description, basePrice, minIncrement,
        sellerId, imageUrl, available, listedAt,
        vehicleType, make, model, year, mileage, fuelType, transmission,
        color, licensePlate, hasValidRegistry);
  }

  /**
   * Generic create từ category string — dùng khi parse từ JSON.
   * Trả về Item chung chung (polymorphism).
   *
   * 🎓 Tại sao trả về Item mà không phải subclass cụ thể?
   * → Vì code gọi thường không cần biết chi tiết subclass,
   *   chỉ cần xử lý qua interface của Item (getCategory, basePrice, ...).
   *
   * @param category chuỗi danh mục: "ELECTRONICS", "ART", "VEHICLE"
   * @return item tương ứng (cần cast nếu cần truy cập field đặc thù)
   */
  public static Item createByCategory(String category) {
    // Chuyển String → Enum để validate tên danh mục
    ItemCategory.fromString(category); // Ném lỗi nếu category không hợp lệ
    // Luôn ném exception vì method này yêu cầu nhiều tham số hơn
    throw new UnsupportedOperationException(
        "Dùng createElectronics() / createArt() / createVehicle() với đầy đủ params. "
            + "Category nhận được: " + category);
  }
}
