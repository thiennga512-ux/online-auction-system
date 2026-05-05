package com.auction.model;

import com.auction.model.Item;
import com.auction.model.ItemData;

public interface ItemFactory {

    // 🔥 nhận DTO → tạo object
    Item createItem(ItemData data);
}