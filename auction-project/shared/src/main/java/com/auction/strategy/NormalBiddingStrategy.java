package com.auction.strategy;

import com.auction.model.Bidder;
import com.auction.service.Auction;

/**
 * NormalBiddingStrategy: Chiến lược đặt giá thủ công thông thường.
 */
public class NormalBiddingStrategy implements BiddingStrategy {
    @Override
    public void placeBid(Bidder bidder, Auction auction, double amount) {
        System.out.println("--- Đặt giá thủ công ---");
        auction.placeNewBid(bidder, amount);
    }
}
