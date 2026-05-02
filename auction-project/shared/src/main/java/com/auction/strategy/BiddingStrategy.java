package com.auction.strategy;

import com.auction.model.Bidder;
import com.auction.service.Auction;

/**
 * BiddingStrategy: Interface cho chiến lược đặt giá.
 * Áp dụng Strategy Pattern.
 */
public interface BiddingStrategy {
    /**
     * Thực hiện việc đặt giá dựa trên chiến lược cụ thể.
     * @param bidder Người đặt giá
     * @param auction Phiên đấu giá
     * @param amount Số tiền (nếu là đặt giá thủ công)
     */
    void placeBid(Bidder bidder, Auction auction, double amount);
}
