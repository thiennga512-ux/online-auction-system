package com.auction.model;

//DTO (Data Transfer Object)
// 👉 class này CHỈ chứa dữ liệu, KHÔNG có logic
public class ItemData {

    public String id;
    public String name;
    public String description;
    public double price;

    // dữ liệu riêng cho từng loại (có thể null nếu không dùng)
    public Integer warranty;     // cho Electronics
    public String artistName;    // cho Art

    public ItemData(String id, String name, String description, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
    }
}