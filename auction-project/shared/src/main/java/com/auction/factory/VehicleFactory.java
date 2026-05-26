package com.auction.factory;

import com.auction.model.Item;
import com.auction.model.Vehicle;
import com.auction.enums.ItemCategory;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Factory chuyên biệt để tạo và tái tạo các sản phẩm thuộc danh mục Phương tiện (Vehicle).
 * Đảm bảo nguyên tắc Single Responsibility (SOLID).
 */
public class VehicleFactory extends SubItemFactory {

    @Override
    public Item create(String name, String description, double price, double inc, 
                       String img, String sId, Map<String, Object> data) {
        
        return new Vehicle(
            name, 
            description, 
            price, 
            inc, 
            img, 
            sId, 
            ItemCategory.VEHICLE, // Category mặc định cho factory này
            getStr(data, "make"),
            getStr(data, "model"),
            getInt(data, "year"),
            getInt(data, "mileage"),
            parseEnum(Vehicle.FuelType.class, data.get("fuelType"))
        );
    }

    @Override
    public Item reconstruct(String id, LocalDateTime cAt, LocalDateTime uAt, String name, 
                             String desc, double price, double inc, String img, String sId, Map<String, Object> data) {
        
        return new Vehicle(
            id, 
            cAt, 
            uAt, 
            name, 
            desc, 
            price, 
            inc, 
            img, 
            sId, 
            ItemCategory.VEHICLE,
            getStr(data, "make"),
            getStr(data, "model"),
            getInt(data, "year"),
            getInt(data, "mileage"),
            parseEnum(Vehicle.FuelType.class, data.get("fuelType"))
        );
    }
}