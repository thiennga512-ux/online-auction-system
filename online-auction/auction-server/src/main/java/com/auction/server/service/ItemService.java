package com.auction.server.service;

import com.auction.common.exception.DatabaseException;
import com.auction.common.exception.ValidationException;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemCategory;
import com.auction.server.dao.ItemDAO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * Class ItemService — Business Logic cho Item
 * ============================================================
 *
 * 🎓 GIẢI THÍCH:
 * ItemService chứa các quy tắc nghiệp vụ liên quan đến sản phẩm:
 *   - Validate trước khi đăng sản phẩm (giá, bước giá hợp lệ)
 *   - Kiểm tra Seller có quyền đăng sản phẩm đó không
 *   - Lấy danh sách sản phẩm theo nhiều tiêu chí
 *   - Đánh dấu sản phẩm đã bán khi phiên đấu giá kết thúc
 *
 * Sơ đồ luồng đăng sản phẩm:
 *   Seller → Client → [Socket] → ServerMain → Controller → ItemService → ItemDAO → SQLite
 * ============================================================
 */
public class ItemService {

  private final ItemDAO itemDAO;

  public ItemService(ItemDAO itemDAO) {
    this.itemDAO = itemDAO;
  }

  // -------------------------------------------------------
  // ĐĂNG SẢN PHẨM
  // -------------------------------------------------------

  /**
   * Đăng sản phẩm mới lên hệ thống.
   * Item đã được tạo sẵn từ Factory, Service chỉ validate và lưu.
   *
   * @param item sản phẩm cần đăng (đã tạo từ ItemFactory)
   * @throws IllegalArgumentException nếu sản phẩm không hợp lệ
   */
  public void listItem(Item item) {
    // Gọi validate() của từng subclass → ĐA HÌNH trong action!
    item.validate();
    validatePricing(item);

    try {
      itemDAO.save(item);
      System.out.printf("[ItemService] Đăng sản phẩm mới: %s (Seller: %s)%n",
          item.getName(), item.getSellerId());
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("lưu sản phẩm", e);
    }
  }

  /**
   * Validate quy tắc giá:
   *   - basePrice > 0 (đã check trong constructor)
   *   - minIncrement >= 0.5% của basePrice (tránh bước giá quá nhỏ)
   *   - minIncrement <= 10% của basePrice (tránh bước giá quá lớn)
   */
  private void validatePricing(Item item) {
    double minReasonableIncrement = item.getBasePrice() * 0.005; // 0.5%
    double maxReasonableIncrement = item.getBasePrice() * 0.10;  // 10%

    if (item.getMinIncrement() < minReasonableIncrement) {
      throw new ValidationException("minIncrement", String.format(
          "Bước giá %.0f VND quá nhỏ. Tối thiểu: %.0f VND (0.5%% giá khởi điểm)",
          item.getMinIncrement(), minReasonableIncrement));
    }
    if (item.getMinIncrement() > maxReasonableIncrement) {
      throw new ValidationException("minIncrement", String.format(
          "Bước giá %.0f VND quá lớn. Tối đa: %.0f VND (10%% giá khởi điểm)",
          item.getMinIncrement(), maxReasonableIncrement));
    }
  }

  // -------------------------------------------------------
  // TÌM KIẾM
  // -------------------------------------------------------

  /**
   * Tìm sản phẩm theo ID.
   *
   * @param id ID sản phẩm
   * @return Optional<Item>
   */
  public Optional<Item> findById(String id) {
    try {
      return itemDAO.findById(id);
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("tìm sản phẩm", e);
    }
  }

  /**
   * Lấy tất cả sản phẩm của một Seller.
   *
   * @param sellerId ID Seller
   * @return danh sách Item
   */
  public List<Item> getItemsBySeller(String sellerId) {
    try {
      return itemDAO.findBySellerId(sellerId);
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("lấy sản phẩm theo Seller", e);
    }
  }

  /**
   * Lấy sản phẩm theo danh mục.
   *
   * @param category ELECTRONICS, ART hoặc VEHICLE
   * @return danh sách Item
   */
  public List<Item> getItemsByCategory(ItemCategory category) {
    try {
      return itemDAO.findByCategory(category);
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("lấy sản phẩm theo danh mục", e);
    }
  }

  /**
   * Lấy tất cả sản phẩm còn available (chưa được đưa vào đấu giá / chưa bán).
   *
   * @return danh sách Item available
   */
  public List<Item> getAllAvailableItems() {
    try {
      return itemDAO.findAllAvailable();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database: " + e.getMessage(), e);
    }
  }

  // -------------------------------------------------------
  // CẬP NHẬT
  // -------------------------------------------------------

  /**
   * Đánh dấu sản phẩm là đã bán.
   * Được gọi bởi AuctionService khi phiên kết thúc có người thắng.
   *
   * @param itemId ID sản phẩm đã bán
   */
  public void markItemAsSold(String itemId) {
    try {
      itemDAO.markAsSold(itemId);
      System.out.println("[ItemService] Sản phẩm đã bán: " + itemId);
    } catch (SQLException e) {
      throw DatabaseException.queryFailed("đánh dấu sản phẩm đã bán", e);
    }
  }
}
