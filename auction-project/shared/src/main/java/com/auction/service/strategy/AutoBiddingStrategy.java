package com.auction.service.strategy;

import com.auction.model.Bidder;
import com.auction.service.auction.Auction;

/**
 * AutoBiddingStrategy: Chiến lược đặt giá tự động.
 * Tự động tăng giá khi có người khác đặt giá cao hơn, cho đến giới hạn tối đa.
 */
public class AutoBiddingStrategy implements BiddingStrategy {
    private double maxLimit;
    private double increment;

    public AutoBiddingStrategy(double maxLimit, double increment) {
        this.maxLimit = maxLimit;
        this.increment = increment;
    }

    @Override
    public void placeBid(Bidder bidder, Auction auction, double amount) {
        double currentHighest = auction.getHighestBid();
        double nextBid = currentHighest + increment;

        if (nextBid <= maxLimit && nextBid <= bidder.getBalance()) {
            System.out.println("--- Đặt giá tự động (" + bidder.getUsername() + ") ---");
            auction.placeNewBid(bidder, nextBid);
        } else {
            System.out.println("--- Dừng đặt giá tự động cho " + bidder.getUsername() + " (Vượt giới hạn hoặc hết tiền) ---");
        }
    }
}
