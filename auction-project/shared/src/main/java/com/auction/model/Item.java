package com.auction.model;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Item extends UserRole {

    protected String name;
    protected String description;//Mô tả
    protected double startingPrice;//Giá khởi điểm

    public Item(String id, String name, String description, double startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getName() {
        return name;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    // 🔥 POLYMORPHISM
    // mỗi loại item tự định nghĩa cách in riêng
    public abstract void printInfo();
}