package com.auction.model;

/**
 * Lớp trừu tượng Item: Đại diện cho một vật phẩm được đem ra đấu giá.
 */
public abstract class Item extends Entity {

    protected String name;
    protected String description; // Mô tả
    protected double startingPrice; // Giá khởi điểm

    public Item(String id, String name, String description, double startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    // 🔥 POLYMORPHISM
    // mỗi loại item tự định nghĩa cách in riêng
    public abstract void printInfo();
}