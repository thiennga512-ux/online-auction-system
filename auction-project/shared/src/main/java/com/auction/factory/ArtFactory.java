package com.auction.factory;

import com.auction.model.Art;
import com.auction.model.Item;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Factory chuyên biệt cho danh mục Tác phẩm Nghệ thuật (Art).
 * Quản lý các thuộc tính đặc thù như Artist, Year Created và Medium.
 */
public class ArtFactory extends SubItemFactory {

    @Override
    public Item create(String name, String description, double price, double inc, 
                       String img, String sId, Map<String, Object> data) {
        
        return new Art(
            name, 
            description, 
            price, 
            inc, 
            img, 
            sId,
            getStr(data, "artistName"),
            getInt(data, "yearCreated"),
            getStr(data, "medium")
        );
    }

    @Override
    public Item reconstruct(String id, LocalDateTime cAt, LocalDateTime uAt, String name, 
                             String desc, double price, double inc, String img, String sId, Map<String, Object> data) {
        
        return new Art(
            id, 
            cAt, 
            uAt, 
            name, 
            desc, 
            price, 
            inc, 
            img, 
            sId,
            getStr(data, "artistName"),
            getInt(data, "yearCreated"),
            getStr(data, "medium")
        );
    }
}