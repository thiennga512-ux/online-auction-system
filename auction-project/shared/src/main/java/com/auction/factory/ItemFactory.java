package com.auction.factory;

import com.auction.model.*;
import com.auction.enums.ItemCategory;
import java.time.LocalDateTime;
import java.util.Map;

public class ItemFactory {

    /**
     * HÀM 1: TẠO MỚI SẢN PHẨM (Dùng khi Seller đăng đồ mới)
     * Tự động dùng Constructor tạo mới (ID tự sinh trong BaseEntity/Item)
     */
    public static Item createNewItem(ItemCategory category, String name, String description, 
                                     double startingPrice, double bidIncrement, String imageUrl, 
                                     String sellerId, Map<String, Object> extraData) {
        
        return switch (category) {
            case ELECTRONICS -> new Electronics(
                name, description, startingPrice, bidIncrement, imageUrl, sellerId,
                (String) extraData.get("brand"),
                (String) extraData.get("model"),
                (int) extraData.get("warrantyMonths"),
                (Electronics.Condition) extraData.get("condition")
            );

            case ART -> new Art(
                name, description, startingPrice, bidIncrement, imageUrl, sellerId,
                (String) extraData.get("artistName"),
                (int) extraData.get("yearCreated"),
                (String) extraData.get("medium")
            );


            default -> new Item(name, description, startingPrice, bidIncrement, imageUrl, sellerId, category);
        };
    }

    /**
     * HÀM 2: TÁI TẠO TỪ DATABASE (Dùng khi đọc dữ liệu từ MySQL lên)
     * Giữ nguyên ID, ngày tạo, ngày cập nhật cũ.
     */
    public static Item rebuildFromDb(ItemCategory category, String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                                     String name, String description, double startingPrice, double bidIncrement, 
                                     String imageUrl, String sellerId, Map<String, Object> extraData) {
        
        return switch (category) {
            case ELECTRONICS -> new Electronics(
                id, createdAt, updatedAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId,
                (String) extraData.get("brand"),
                (String) extraData.get("model"),
                (int) extraData.get("warrantyMonths"),
                (Electronics.Condition) extraData.get("condition")
            );

            case ART -> new Art(
                id, createdAt, updatedAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId,
                (String) extraData.get("artistName"),
                (int) extraData.get("yearCreated"),
                (String) extraData.get("medium")
            );

            default -> new Item(id, createdAt, updatedAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId, category);
        };
    }
}