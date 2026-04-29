package com.auction.model;

public class ElectronicsFactory implements ItemFactory {

    @Override
    public Item createItem(ItemData data) {

        // kiểm tra dữ liệu cần thiết
        if (data.warranty == null) {
            throw new IllegalArgumentException("Electronics cần warranty");
        }

        // 🔥 tạo object từ DTO
        return new Electronics(
                data.id,
                data.name,
                data.description,
                data.price,
                data.warranty
        );
    }
}