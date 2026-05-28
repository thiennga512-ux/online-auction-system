package com.auction.factory;

import com.auction.model.Item;
import java.time.LocalDateTime;
import java.util.Map;

public abstract class SubItemFactory {
    
    // Tạo mới (Seller đăng bài)
    public abstract Item create(String name, String description, double price, 
                                double inc, String img, String sId, Map<String, Object> data);

    // Tái tạo (Từ Database)
    public abstract Item reconstruct(String id, LocalDateTime cAt, LocalDateTime uAt, String name, 
                                     String desc, double price, double inc, String img, String sId, Map<String, Object> data);

    // --- Helper Methods (Tránh lặp code ép kiểu) ---
    protected String getStr(Map<String, Object> data, String key) {
        return (String) data.getOrDefault(key, "");
    }

    protected int getInt(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) return Integer.parseInt(s);
        return 0;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Enum<T>> T parseEnum(Class<T> enumClass, Object value) {
        if (value == null) return null;
        if (enumClass.isInstance(value)) return (T) value;
        if (value instanceof String s) return Enum.valueOf(enumClass, s.toUpperCase());
        return null;
    }
}