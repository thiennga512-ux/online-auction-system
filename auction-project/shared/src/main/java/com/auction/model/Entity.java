package com.auction.model;

import java.util.UUID;

abstract class Entity {

    protected String id;

    // Constructor rỗng (dùng cho framework / database)
    public Entity() {
        this.id = UUID.randomUUID().toString();//cho các id ngẫu nhiên
    }

    // Constructor có tham số
    public Entity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}