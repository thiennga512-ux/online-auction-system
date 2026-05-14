package com.auction.model;
import java.time.LocalDateTime;

import com.auction.enums.ItemCategory;

public class Vehicle extends Item {
    public enum FuelType {
    GASOLINE("Xăng"),
    DIESEL("Dầu diesel"),
    ELECTRIC("Điện"),
    HYBRID("Hybrid"),
    OTHER("Khác");
    private final String display;
    public String getDisplay() { return display; }
    FuelType(String display) {
        this.display = display;
    }
}
    
    private String make; // Hãng sản xuất
    private String model; // Mẫu xe
    private int year;     // Năm sản xuất
    private int mileage;  // Số km đã đi
    private FuelType fuelType; // Loại nhiên liệu

    public Vehicle(String name, String description, double startingPrice, double bidIncrement,
                   String imageUrl, String sellerId, ItemCategory category,
                   String make, String model, int year, int mileage, FuelType fuelType) {
        super(name, description, startingPrice, bidIncrement, imageUrl, sellerId, category);
        this.make = make;
        this.model = model;
        this.year = year;
        this.mileage = mileage;
        this.fuelType = fuelType;
    }

    public Vehicle(String id, LocalDateTime createdAt, LocalDateTime updateAt,
                   String name, String description, double startingPrice, double bidIncrement,
                   String imageUrl, String sellerId, ItemCategory category,
                   String make, String model, int year, int mileage, FuelType fuelType) {
        super(id, createdAt, updateAt, name, description, startingPrice, bidIncrement,
              imageUrl, sellerId, category);
        this.make = make;
        this.model = model;
        this.year = year;
        this.mileage = mileage;
        this.fuelType = fuelType;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  -> Xe: %s %s (%d) | %d km | Nhiên liệu: %s%n",
                make, model, year, mileage, fuelType.getDisplay());
    }
    // --- Getters & Setters ---
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }
}

