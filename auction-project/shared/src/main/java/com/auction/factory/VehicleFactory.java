package com.auction.factory;

import com.auction.model.Item;
import com.auction.model.Vehicle;
import com.auction.enums.ItemCategory;
import java.time.LocalDateTime;
import java.util.Map;

public class VehicleFactory extends SubItemFactory {

    @Override
    public Item create(String name, String description, double price, double inc,
            String img, String sId, Map<String, Object> data) {

        return new Vehicle(name, description, price, inc, img, sId, ItemCategory.VEHICLE,
                getStr(data, "vehicleType"),
                getStr(data, "make"),
                getStr(data, "model"),
                getInt(data, "year"),
                getInt(data, "mileage"),
                parseEnum(Vehicle.FuelType.class, data.get("fuelType")),
                getStr(data, "transmission"),
                getStr(data, "color"),
                getStr(data, "licensePlate"),
                getBool(data, "hasValidRegistry"));
    }

    @Override
    public Item reconstruct(String id, LocalDateTime cAt, LocalDateTime uAt, String name,
            String desc, double price, double inc, String img, String sId, Map<String, Object> data) {

        return new Vehicle(id, cAt, uAt, name, desc, price, inc, img, sId, ItemCategory.VEHICLE,
                getStr(data, "vehicleType"),
                getStr(data, "make"),
                getStr(data, "model"),
                getInt(data, "year"),
                getInt(data, "mileage"),
                parseEnum(Vehicle.FuelType.class, data.get("fuelType")),
                getStr(data, "transmission"),
                getStr(data, "color"),
                getStr(data, "licensePlate"),
                getBool(data, "hasValidRegistry"));
    }
}