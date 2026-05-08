package com.auction.factory;

import com.auction.model.Item;

/**
 * Interface ItemFactory: Định nghĩa khuôn mẫu để tạo các loại Item khác nhau.
 * Áp dụng Factory Method Pattern.
 */
public interface ItemFactory {

    // 🔥 Nhận DTO → Tạo đối tượng cụ thể (Art, Electronics, ...)
    Item createItem(ItemData data);
}