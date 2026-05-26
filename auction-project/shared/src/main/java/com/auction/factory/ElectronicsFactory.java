package com.auction.factory;

import com.auction.model.Electronics;
import com.auction.model.Item;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Factory chuyên biệt cho danh mục Đồ điện tử (Electronics).
 * Giải quyết bài toán khởi tạo phức tạp và ép kiểu dữ liệu từ Map.
 */
public class ElectronicsFactory extends SubItemFactory {

    @Override
    public Item create(String name, String description, double price, double inc, 
                       String img, String sId, Map<String, Object> data) {
        
        return new Electronics(
            name, 
            description, 
            price, 
            inc, 
            img, 
            sId,
            getStr(data, "brand"),
            getStr(data, "model"),
            getInt(data, "warrantyMonths"),
            parseEnum(Electronics.Condition.class, data.get("condition"))
        );
    }

    @Override
    public Item reconstruct(String id, LocalDateTime cAt, LocalDateTime uAt, String name, 
                             String desc, double price, double inc, String img, String sId, Map<String, Object> data) {
        
        return new Electronics(
            id, 
            cAt, 
            uAt, 
            name, 
            desc, 
            price, 
            inc, 
            img, 
            sId,
            getStr(data, "brand"),
            getStr(data, "model"),
            getInt(data, "warrantyMonths"),
            parseEnum(Electronics.Condition.class, data.get("condition"))
        );
    }
}
