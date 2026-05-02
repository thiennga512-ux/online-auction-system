package com.auction.model;

import com.auction.service.Observer;
import com.auction.strategy.BiddingStrategy;
import java.time.LocalDateTime;

/**
 * Bidder: Người tham gia đấu giá.
 * Kế thừa User và triển khai Observer để nhận thông báo thời gian thực.
 */
public class Bidder extends User implements Observer {
    private double balance; // Số dư tài khoản
    private BiddingStrategy strategy; // Chiến lược đặt giá

    public Bidder(String id, String fullName, String username, String email, String password, String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt, boolean active, double balance) {
        super(id, fullName, username, email, password, phoneNumber, gender, dateOfBirth, createdAt, active);
        this.balance = balance;
    }

    // Constructor rút gọn cho tạo mới
    public Bidder(String fullName, String username, String email, String password, String gender, String dateOfBirth, double balance) {
        super(fullName, username, email, password, gender, dateOfBirth);
        this.balance = balance;
    }

    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
    }

    @Override
    public String getDashboardView() {
        return "--- GIAO DIỆN NGƯỜI ĐẶT GIÁ ---";
    }

    public void placeBid(double amount) {
        System.out.println(">>> " + getUsername() + " quyết định đặt giá: " + amount + " VNĐ");
    }

    @Override
    public void update(String message, com.auction.service.Auction auction) {
        System.out.println("[THÔNG BÁO tới " + getUsername() + "]: " + message);
        
        // Nếu có strategy và mình không phải là người đang giữ giá cao nhất
        if (strategy != null && auction.isOngoing() && (auction.getWinner() == null || !auction.getWinner().equals(this))) {
            strategy.placeBid(this, auction, 0);
        }
    }

    public BiddingStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(BiddingStrategy strategy) {
        this.strategy = strategy;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}