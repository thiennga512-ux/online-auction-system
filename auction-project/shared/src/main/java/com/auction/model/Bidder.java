package com.auction.model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import com.auction.enums.*;
import com.auction.service.auction.*;

public class Bidder extends User {
    private double balance;
    private double frozenBalance;
    private String shippingAddress;

    // Lưu danh sách các phiên để truy xuất trạng thái thắng/thua
    private List<AuctionSession> participatedAuctions; 
    private List<String> watchlist;

    // CONSTRUCTOR 1: Đăng ký mới
    public Bidder(String username, String passwordHash, String email, String fullName) {
        super(username, passwordHash, email, fullName, UserRole.BIDDER);
        this.balance = 0.0;
        this.frozenBalance = 0.0;
        this.participatedAuctions = new ArrayList<>();
        this.watchlist = new ArrayList<>();
    }

    // CONSTRUCTOR 2: Load từ Database 
    public Bidder(String id, LocalDateTime createdAt, LocalDateTime updatedAt, String username, 
                  String passwordHash, String email, String fullName, boolean active, 
                  double balance, double frozenBalance, String shippingAddress) {
        super(id, createdAt, updatedAt, username, passwordHash, email, fullName, UserRole.BIDDER, active);
        this.balance = balance;
        this.frozenBalance = frozenBalance;
        this.shippingAddress = shippingAddress;
        this.participatedAuctions = new ArrayList<>();
        this.watchlist = new ArrayList<>();
    }

    // ============================================================
    // GETTERS & SETTERS (Đầy đủ cho các thuộc tính)
    // ============================================================

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getFrozenBalance() {
        return frozenBalance;
    }

    public void setFrozenBalance(double frozenBalance) {
        this.frozenBalance = frozenBalance;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<AuctionSession> getParticipatedAuctions() {
        return participatedAuctions;
    }

    public void setParticipatedAuctions(List<AuctionSession> participatedAuctions) {
        this.participatedAuctions = participatedAuctions;
    }

    public List<String> getWatchlist() {
        return watchlist;
    }

    public void setWatchlist(List<String> watchlist) {
        this.watchlist = watchlist;
    }

    // ============================================================
    // LOGIC NGHIỆP VỤ (Business Logic)
    // ============================================================

    public void addBalance(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    public boolean canAfford(double amount) {
        return this.balance >= amount;
    }

    public List<AuctionSession> getAuctionHistory() {
        List<AuctionSession> history = new ArrayList<>();
        for (AuctionSession session : participatedAuctions) {
            if (session.getStatus() != AuctionStatus.OPEN && session.getStatus() != AuctionStatus.RUNNING) {
                history.add(session);
            }
        }
        return history;
    }

    public void addParticipatedAuction(AuctionSession session) {
        if (!participatedAuctions.contains(session)) {
            participatedAuctions.add(session);
        }
    }

    public void addToWatchlist(String itemId) {
        if (!watchlist.contains(itemId)) {
            watchlist.add(itemId);
        }
    }

    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
    }

    @Override
    public String getDashboardView() {
        return "/views/bidder_dashboard.fxml";
    }

    public int getTotalBidsPlaced() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTotalBidsPlaced'");
    }
}