package com.auction.service;

import com.auction.model.Item;
import java.util.HashMap;
import java.util.Map;

public class AuctionManager {
    // Lưu các phiên đấu giá theo ID của món hàng
    private Map<Integer, Auction> activeAuctions = new HashMap<>();

    public void startNewAuction(Item item) {
        Auction auction = new Auction(item);
        activeAuctions.put(item.getId(), auction);
        System.out.println("✅ Đã bắt đầu phiên đấu giá cho: " + item.getName());
    }

    public Auction getAuction(int itemId) {
        return activeAuctions.get(itemId);
    }
}