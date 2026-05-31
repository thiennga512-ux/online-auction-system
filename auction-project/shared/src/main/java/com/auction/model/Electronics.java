package com.auction.model;

import java.time.LocalDateTime;
import com.auction.enums.ItemCategory;

public class Electronics extends Item {
    // Enum định nghĩa tình trạng riêng cho đồ điện tử
    public enum Condition {
        NEW("Mới 100%"),
        LIKE_NEW("Như mới (99%)"),
        USED("Đã qua sử dụng"),
        FOR_PARTS("Hỏng - Lấy linh kiện");

        private final String display;

        Condition(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    private String brand;
    private String model;
    private int warrantyMonths;
    private Condition condition;

    // Constructor cho tạo mới: Gán cứng Category.ELECTRONICS
    public Electronics(String name, String description, double startingPrice, double bidIncrement,
            String imageUrl, String sellerId,
            String brand, String model, int warrantyMonths, Condition condition) {

        super(name, description, startingPrice, bidIncrement, imageUrl, sellerId, ItemCategory.ELECTRONICS);
        this.brand = brand;
        this.model = model;
        this.warrantyMonths = warrantyMonths;
        this.condition = condition;
    }

    // Constructor nạp từ MySQL
    public Electronics(String id, LocalDateTime createdAt, LocalDateTime updateAt,
            String name, String description, double startingPrice, double bidIncrement,
            String imageUrl, String sellerId,
            String brand, String model, int warrantyMonths, Condition condition) {

        super(id, createdAt, updateAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId,
                ItemCategory.ELECTRONICS);
        this.brand = brand;
        this.model = model;
        this.warrantyMonths = warrantyMonths;
        this.condition = condition;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ELECTRONICS;
    }

    @Override
    public void printInfo() {
        System.out.println("---------- THIẾT BỊ ĐIỆN TỬ ----------");
        super.printInfo();
        System.out.printf("Thương hiệu: %s | Model: %s%n", brand, model);
        System.out.printf("Tình trạng: %s | Bảo hành: %d tháng%n", condition.getDisplay(), warrantyMonths);
        System.out.println("--------------------------------------");
    }

    // --- Getters & Setters ---
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        if (warrantyMonths < 0)
            throw new IllegalArgumentException("Bảo hành không được âm");
        this.warrantyMonths = warrantyMonths;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }
}