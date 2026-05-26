package com.auction.factory;

import com.auction.model.Item;
import com.auction.enums.ItemCategory;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public class ItemFactory {
    // 1. Quản lý danh sách các Factory con
    private static final Map<ItemCategory, SubItemFactory> registry = new EnumMap<>(ItemCategory.class);

    static {
        // Đăng ký các Factory vào đây
        registry.put(ItemCategory.ELECTRONICS, new ElectronicsFactory());
        registry.put(ItemCategory.VEHICLE, new VehicleFactory());
        registry.put(ItemCategory.ART, new ArtFactory());
    }

    // 2. Phương thức tạo mới cực kỳ ngắn gọn
    public static Item createNewItem(ItemCategory category, String name, String description,
            double price, double inc, String img, String sId, Map<String, Object> data) {
        SubItemFactory factory = registry.get(category);
        if (factory == null)
            return new Item(name, description, price, inc, img, sId, category);

        return factory.create(name, description, price, inc, img, sId, data);
    }

    // 3. Phương thức tái tạo từ DB
    public static Item reconstructFromDb(ItemCategory category, String id, LocalDateTime cAt, LocalDateTime uAt,
            String name, String desc, double price, double inc, String img, String sId, Map<String, Object> data) {
        SubItemFactory factory = registry.get(category);
        if (factory == null)
            return new Item(id, cAt, uAt, name, desc, price, inc, img, sId, category);

        return factory.reconstruct(id, cAt, uAt, name, desc, price, inc, img, sId, data);
    }
}
