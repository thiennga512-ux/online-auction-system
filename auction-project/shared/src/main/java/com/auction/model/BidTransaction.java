package com.auction.model;

import java.time.LocalDateTime;

// Lưu trữ lịch sử một lần đặt giá
public class BidTransaction {
    private Bidder bidder;
    private double amount;
    private LocalDateTime timestamp;

    public BidTransaction(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now(); // Lấy thời gian hiện tại của hệ thống
    }

    public double getAmount() { return amount; }
    public Bidder getBidder() { return bidder; }
    
    @Override
    public String toString() {
        return bidder.getUsername() + " đã đặt " + amount + " VNĐ lúc " + timestamp;
    }
}