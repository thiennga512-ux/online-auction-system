package com.auction.server.dao;

import com.auction.common.factory.ItemFactory;
import com.auction.common.model.item.Art;
import com.auction.common.model.item.Electronics;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemCategory;
import com.auction.common.model.item.Vehicle;
import com.auction.server.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * Class ItemDAO — Data Access Object cho Item (3 loại sản phẩm)
 * ============================================================
 *
 * 🎓 THIẾT KẾ ĐẶC BIỆT — Table Inheritance Strategy:
 *
 * Vì mỗi loại Item có field khác nhau, ta dùng chiến lược:
 * "Single Table Inheritance" + "Detail Table":
 *   - Bảng "items": lưu field CHUNG (id, name, price, category...)
 *   - Bảng "electronics_details": field riêng của Electronics
 *   - Bảng "art_details": field riêng của Art
 *   - Bảng "vehicle_details": field riêng của Vehicle
 *
 * Khi INSERT: 2 bước (items table + detail table)
 * Khi SELECT: JOIN 2 bảng lại thành 1 kết quả
 *
 * Đây là cách ánh xạ OOP Inheritance xuống Relational Database.
 * ============================================================
 */
public class ItemDAO {

  private Connection getConnection() {
    return DatabaseManager.getInstance().getConnection();
  }

  // -------------------------------------------------------
  // CREATE
  // -------------------------------------------------------

  /**
   * Lưu Item mới vào database.
   * Tự động phân nhánh theo category để insert đúng bảng detail.
   *
   * @param item sản phẩm cần lưu
   */
  public void save(Item item) throws SQLException {
    // Bước 1: Insert vào bảng items (thông tin chung)
    String sql = """
        INSERT INTO items
            (id, name, description, base_price, min_increment,
             seller_id, category, image_url, available, listed_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, item.getId());
      ps.setString(2, item.getName());
      ps.setString(3, item.getDescription());
      ps.setDouble(4, item.getBasePrice());
      ps.setDouble(5, item.getMinIncrement());
      ps.setString(6, item.getSellerId());
      ps.setString(7, item.getCategory().name());
      ps.setString(8, item.getImageUrl());
      ps.setBoolean(9, item.isAvailable());
      ps.setString(10, item.getListedAt().toString());
      ps.executeUpdate();
    }

    // Bước 2: Insert vào bảng detail tương ứng
    switch (item.getCategory()) {
      case ELECTRONICS -> saveElectronicsDetails((Electronics) item);
      case ART         -> saveArtDetails((Art) item);
      case VEHICLE     -> saveVehicleDetails((Vehicle) item);
    }
  }

  private void saveElectronicsDetails(Electronics e) throws SQLException {
    String sql = """
        INSERT INTO electronics_details
            (item_id, brand, model, warranty_months, condition_type)
        VALUES (?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, e.getId());
      ps.setString(2, e.getBrand());
      ps.setString(3, e.getModel());
      ps.setInt(4, e.getWarrantyMonths());
      ps.setString(5, e.getCondition().name());
      ps.executeUpdate();
    }
  }

  private void saveArtDetails(Art a) throws SQLException {
    String sql = """
        INSERT INTO art_details
            (item_id, artist_name, creation_year, medium,
             authenticated, certificate_id, dimensions)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, a.getId());
      ps.setString(2, a.getArtistName());
      ps.setInt(3, a.getCreationYear());
      ps.setString(4, a.getMedium());
      ps.setBoolean(5, a.isAuthenticated());
      ps.setString(6, a.getCertificateId());
      ps.setString(7, a.getDimensions());
      ps.executeUpdate();
    }
  }

  private void saveVehicleDetails(Vehicle v) throws SQLException {
    String sql = """
        INSERT INTO vehicle_details
            (item_id, vehicle_type, make, model, year, mileage,
             fuel_type, transmission, color, license_plate, has_valid_registry)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, v.getId());
      ps.setString(2, v.getVehicleType().name());
      ps.setString(3, v.getMake());
      ps.setString(4, v.getModel());
      ps.setInt(5, v.getYear());
      ps.setDouble(6, v.getMileage());
      ps.setString(7, v.getFuelType().name());
      ps.setString(8, v.getTransmission().name());
      ps.setString(9, v.getColor());
      ps.setString(10, v.getLicensePlate());
      ps.setBoolean(11, v.isHasValidRegistry());
      ps.executeUpdate();
    }
  }

  // -------------------------------------------------------
  // READ
  // -------------------------------------------------------

  /**
   * Tìm Item theo ID — JOIN với bảng detail tương ứng.
   *
   * @param id ID sản phẩm
   * @return Optional<Item> (đúng subclass: Electronics/Art/Vehicle)
   */
  public Optional<Item> findById(String id) throws SQLException {
    String sql = "SELECT * FROM items WHERE id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRowToItem(rs));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Lấy tất cả sản phẩm của một Seller.
   *
   * @param sellerId ID người bán
   * @return danh sách Item thuộc seller này
   */
  public List<Item> findBySellerId(String sellerId) throws SQLException {
    List<Item> items = new ArrayList<>();
    String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY listed_at DESC";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          items.add(mapRowToItem(rs));
        }
      }
    }
    return items;
  }

  /**
   * Lấy tất cả sản phẩm theo danh mục.
   *
   * @param category danh mục cần lọc
   * @return danh sách Item thuộc danh mục
   */
  public List<Item> findByCategory(ItemCategory category) throws SQLException {
    List<Item> items = new ArrayList<>();
    String sql = "SELECT * FROM items WHERE category = ? AND available = 1";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, category.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          items.add(mapRowToItem(rs));
        }
      }
    }
    return items;
  }

  /**
   * Lấy tất cả sản phẩm còn available (chưa bán).
   */
  public List<Item> findAllAvailable() throws SQLException {
    List<Item> items = new ArrayList<>();
    String sql = "SELECT * FROM items WHERE available = 1 ORDER BY listed_at DESC";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          items.add(mapRowToItem(rs));
        }
      }
    }
    return items;
  }

  // -------------------------------------------------------
  // UPDATE
  // -------------------------------------------------------

  /**
   * Đánh dấu sản phẩm đã bán (available = false).
   * Được gọi khi phiên đấu giá kết thúc và có người thắng.
   *
   * @param itemId ID sản phẩm
   */
  public void markAsSold(String itemId) throws SQLException {
    String sql = "UPDATE items SET available = false WHERE id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, itemId);
      ps.executeUpdate();
    }
  }

  // -------------------------------------------------------
  // PRIVATE HELPER — Map ResultSet → Item subclass
  // -------------------------------------------------------

  /**
   * Đọc thông tin chung từ items table, sau đó JOIN detail table
   * để tạo đúng subclass (Electronics/Art/Vehicle).
   *
   * 🎓 Đây là điểm thể hiện ĐA HÌNH trong tầng DAO:
   *   mapRowToItem() trả về Item (abstract type)
   *   nhưng object thật bên trong là Electronics/Art/Vehicle
   */
  private Item mapRowToItem(ResultSet rs) throws SQLException {
    String id = rs.getString("id");
    String name = rs.getString("name");
    String description = rs.getString("description");
    double basePrice = rs.getDouble("base_price");
    double minIncrement = rs.getDouble("min_increment");
    String sellerId = rs.getString("seller_id");
    String categoryStr = rs.getString("category");
    String imageUrl = rs.getString("image_url");
    boolean available = rs.getBoolean("available");
    LocalDateTime listedAt = LocalDateTime.parse(rs.getString("listed_at"));

    ItemCategory category = ItemCategory.fromString(categoryStr);

    // Dựa vào category, JOIN với bảng detail và tạo đúng subclass
    return switch (category) {
      case ELECTRONICS -> mapElectronics(id, name, description, basePrice, minIncrement,
          sellerId, imageUrl, available, listedAt);
      case ART -> mapArt(id, name, description, basePrice, minIncrement,
          sellerId, imageUrl, available, listedAt);
      case VEHICLE -> mapVehicle(id, name, description, basePrice, minIncrement,
          sellerId, imageUrl, available, listedAt);
    };
  }

  private Electronics mapElectronics(String id, String name, String desc, double basePrice,
      double minInc, String sellerId, String imgUrl, boolean available,
      LocalDateTime listedAt) throws SQLException {
    String sql = "SELECT * FROM electronics_details WHERE item_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return ItemFactory.reconstructElectronics(id, name, desc, basePrice, minInc,
              sellerId, imgUrl, available, listedAt,
              rs.getString("brand"),
              rs.getString("model"),
              rs.getInt("warranty_months"),
              rs.getString("condition_type"));
        }
      }
    }
    throw new SQLException("Không tìm thấy electronics_details cho item: " + id);
  }

  private Art mapArt(String id, String name, String desc, double basePrice,
      double minInc, String sellerId, String imgUrl, boolean available,
      LocalDateTime listedAt) throws SQLException {
    String sql = "SELECT * FROM art_details WHERE item_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return ItemFactory.reconstructArt(id, name, desc, basePrice, minInc,
              sellerId, imgUrl, available, listedAt,
              rs.getString("artist_name"),
              rs.getInt("creation_year"),
              rs.getString("medium"),
              rs.getBoolean("authenticated"),
              rs.getString("certificate_id"),
              rs.getString("dimensions"));
        }
      }
    }
    throw new SQLException("Không tìm thấy art_details cho item: " + id);
  }

  private Vehicle mapVehicle(String id, String name, String desc, double basePrice,
      double minInc, String sellerId, String imgUrl, boolean available,
      LocalDateTime listedAt) throws SQLException {
    String sql = "SELECT * FROM vehicle_details WHERE item_id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return ItemFactory.reconstructVehicle(id, name, desc, basePrice, minInc,
              sellerId, imgUrl, available, listedAt,
              rs.getString("vehicle_type"),
              rs.getString("make"),
              rs.getString("model"),
              rs.getInt("year"),
              rs.getDouble("mileage"),
              rs.getString("fuel_type"),
              rs.getString("transmission"),
              rs.getString("color"),
              rs.getString("license_plate"),
              rs.getBoolean("has_valid_registry"));
        }
      }
    }
    throw new SQLException("Không tìm thấy vehicle_details cho item: " + id);
  }
}
