package com.auction.model;

import java.util.ArrayList;
import java.util.List;

class Seller extends User {

    private double rating; // điểm uy tín
    private List<Item> items; // danh sách sản phẩm đã tạo

    public Seller(String id, String username, String password, String email, double rating) {
        super(id, username, password, email);
        this.rating = rating;
        this.items = new ArrayList<>();
    }

    public double getRating() {
        return rating;
    }

    // tạo sản phẩm
     public Item createItem(ItemFactory factory, ItemData data) {

        // tạo item từ factory + dữ liệu
        Item item = factory.createItem(data);

        items.add(item);

        System.out.println(username + " đã tạo sản phẩm: " + item.getName());

        return item;
    }

    public List<Item> getItems() {
        return items;
    }
}