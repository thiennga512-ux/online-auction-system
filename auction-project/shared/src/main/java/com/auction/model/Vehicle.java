package com.auction.model;
import java.time.LocalDateTime;

import com.auction.enums.ItemCategory;

public class Vehicle extends Item {
    public enum FuelType {
        GASOLINE("Xăng"),
        PETROL("Xăng"),
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
    
    private String vehicleType;
    private String make; // Hãng sản xuất
    private String model; // Mẫu xe
    private int year;     // Năm sản xuất
    private int mileage;  // Số km đã đi
    private FuelType fuelType; // Loại nhiên liệu
    private String transmission;
    private String color;
    private String licensePlate;
    private boolean hasValidRegistry;

    public Vehicle(String name, String description, double startingPrice, double bidIncrement,
                   String imageUrl, String sellerId, ItemCategory category,
                   String vehicleType, String make, String model, int year, int mileage, FuelType fuelType,
                   String transmission, String color, String licensePlate, boolean hasValidRegistry) {
        super(name, description, startingPrice, bidIncrement, imageUrl, sellerId, category);
        this.vehicleType = vehicleType;
        this.make = make;
        this.model = model;
        this.year = year;
        this.mileage = mileage;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.color = color;
        this.licensePlate = licensePlate;
        this.hasValidRegistry = hasValidRegistry;
    }

    public Vehicle(String id, LocalDateTime createdAt, LocalDateTime updateAt,
                   String name, String description, double startingPrice, double bidIncrement,
                   String imageUrl, String sellerId, ItemCategory category,
                   String vehicleType, String make, String model, int year, int mileage, FuelType fuelType,
                   String transmission, String color, String licensePlate, boolean hasValidRegistry) {
        super(id, createdAt, updateAt, name, description, startingPrice, bidIncrement,
              imageUrl, sellerId, category);
        this.vehicleType = vehicleType;
        this.make = make;
        this.model = model;
        this.year = year;
        this.mileage = mileage;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.color = color;
        this.licensePlate = licensePlate;
        this.hasValidRegistry = hasValidRegistry;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  -> Xe: %s %s %s (%d) | %d km | Nhiên liệu: %s | Hộp số: %s%n",
                vehicleType, make, model, year, mileage, fuelType.getDisplay(), transmission);
    }
    
    // --- Getters & Setters ---
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

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

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public boolean isHasValidRegistry() { return hasValidRegistry; }
    public void setHasValidRegistry(boolean hasValidRegistry) { this.hasValidRegistry = hasValidRegistry; }
}
