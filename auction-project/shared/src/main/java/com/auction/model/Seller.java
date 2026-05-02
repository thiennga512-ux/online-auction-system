package com.auction.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seller: Người bán hàng.
 */
public class Seller extends User {

    private double rating; // Điểm uy tín
    private List<Item> items; // Danh sách sản phẩm đã tạo

    public Seller(String id, String fullName, String username, String email, String password, String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt, boolean active, double rating) {
        super(id, fullName, username, email, password, phoneNumber, gender, dateOfBirth, createdAt, active);
        this.rating = rating;
        this.items = new ArrayList<>();
    }

    public Seller(String fullName, String username, String email, String password, String gender, String dateOfBirth, double rating) {
        super(fullName, username, email, password, gender, dateOfBirth);
        this.rating = rating;
        this.items = new ArrayList<>();
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    @Override
    public String getDashboardView() {
        return "--- GIAO DIỆN NGƯỜI BÁN ---";
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    // Tạo sản phẩm bằng Factory Pattern
    public Item createItem(ItemFactory factory, ItemData data) {
        Item item = factory.createItem(data);
        items.add(item);
        System.out.println(getUsername() + " đã tạo sản phẩm: " + item.getName());
        return item;
    }

    public List<Item> getItems() {
        return items;
    }
}