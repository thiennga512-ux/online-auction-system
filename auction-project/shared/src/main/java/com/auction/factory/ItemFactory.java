package com.auction.factory;

import com.auction.model.*;
import com.auction.enums.ItemCategory;
import java.time.LocalDateTime;
import java.util.Map;

public class ItemFactory {

    private ItemFactory() {
        throw new UnsupportedOperationException("ItemFactory là Utility Class");
    }

    // -------------------------------------------------------
    // 1. TẠO MỚI SẢN PHẨM (Dùng cho Seller đăng đồ)
    // -------------------------------------------------------
    public static Item createNewItem(ItemCategory category, String name, String description,
                                     double startingPrice, double bidIncrement, String imageUrl,
                                     String sellerId, Map<String, Object> extraData) {

        return switch (category) {
            case ELECTRONICS -> new Electronics(
                name, description, startingPrice, bidIncrement, imageUrl, sellerId,
                (String) extraData.get("brand"),
                (String) extraData.get("model"),
                ((Number) extraData.get("warrantyMonths")).intValue(),
                parseEnum(Electronics.Condition.class, extraData.get("condition"))
            );

            case ART -> new Art(
                name, description, startingPrice, bidIncrement, imageUrl, sellerId,
                (String) extraData.get("artistName"),
                ((Number) extraData.get("yearCreated")).intValue(),
                (String) extraData.get("medium")
            );

            case VEHICLE -> new Vehicle(
                name, description, startingPrice, bidIncrement, imageUrl, sellerId, category,
                (String) extraData.get("make"),
                (String) extraData.get("model"),
                ((Number) extraData.get("year")).intValue(),
                ((Number) extraData.get("mileage")).intValue(),
                parseEnum(Vehicle.FuelType.class, extraData.get("fuelType"))
            );

            default -> new Item(name, description, startingPrice, bidIncrement, imageUrl, sellerId, category);
        };
    }

    // -------------------------------------------------------
    // 2. TÁI TẠO TỪ DATABASE (Reconstruct)
    // -------------------------------------------------------
    public static Item reconstructFromDb(ItemCategory category, String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                                         String name, String description, double startingPrice, double bidIncrement,
                                         String imageUrl, String sellerId, Map<String, Object> extraData) {

        return switch (category) {
            case ELECTRONICS -> new Electronics(
                id, createdAt, updatedAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId,
                (String) extraData.get("brand"),
                (String) extraData.get("model"),
                ((Number) extraData.get("warrantyMonths")).intValue(),
                parseEnum(Electronics.Condition.class, extraData.get("condition"))
            );

            case ART -> new Art(
                id, createdAt, updatedAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId,
                (String) extraData.get("artistName"),
                ((Number) extraData.get("yearCreated")).intValue(),
                (String) extraData.get("medium")
            );

            case VEHICLE -> new Vehicle(
                id, createdAt, updatedAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId, category,
                (String) extraData.get("make"),
                (String) extraData.get("model"),
                ((Number) extraData.get("year")).intValue(),
                ((Number) extraData.get("mileage")).intValue(),
                parseEnum(Vehicle.FuelType.class, extraData.get("fuelType"))
            );

            default -> new Item(id, createdAt, updatedAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId, category);
        };
    }

    // -------------------------------------------------------
    // HELPER METHODS (Xử lý ép kiểu an toàn cho GSON & Enum)
    // -------------------------------------------------------

    /**
     * Helper để chuyển đổi giá trị từ Map sang Enum an toàn.
     * Xử lý được cả trường hợp giá trị là String hoặc đã là Enum object.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> T parseEnum(Class<T> enumClass, Object value) {
        if (value == null) return null;
        if (enumClass.isInstance(value)) {
            return (T) value;
        }
        if (value instanceof String) {
            return Enum.valueOf(enumClass, ((String) value).toUpperCase());
        }
        return null;
    }
}
