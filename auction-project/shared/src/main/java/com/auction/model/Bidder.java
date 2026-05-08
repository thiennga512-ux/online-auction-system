package com.auction.model;

import java.util.ArrayList;
import java.util.List;
import com.auction.enums.*;
import com.auction.service.auction.*;


public class Bidder extends User {
    private double balance;
    private double frozenBalance;
    private String shippingAddress;

    // Lưu danh sách các phiên để truy xuất trạng thái thắng/thua
    private List<AuctionSession> participatedAuctions; 
    private List<String> watchlist;

    public Bidder(String username, String passwordHash, String email, String fullName) {
        super(username, passwordHash, email, fullName, UserRole.BIDDER);
        this.balance = 0.0;
        this.frozenBalance = 0.0;
        this.participatedAuctions = new ArrayList<>();
        this.watchlist = new ArrayList<>();
    }

    /**
     * Phương thức để lấy ra các phiên đã kết thúc (Lịch sử)
     */
    public List<AuctionSession> getAuctionHistory() {
        List<AuctionSession> history = new ArrayList<>();
        for (AuctionSession session : participatedAuctions) {
            // Nếu trạng thái không phải là OPEN hoặc RUNNING thì coi là lịch sử
            if (session.getStatus() != AuctionStatus.OPEN && session.getStatus() != AuctionStatus.RUNNING) {
                history.add(session);
            }
        }
        return history;
    }

    // ============================================================
    // GETTERS & SETTERS
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
    // CONVENIENCE METHODS (Các phương thức tiện ích)
    // ============================================================

    /**
     * Thêm một phiên vào danh sách đã tham gia (nếu chưa có)
     */
    public void addParticipatedAuction(AuctionSession session) {
        if (!participatedAuctions.contains(session)) {
            participatedAuctions.add(session);
        }
    }

    /**
     * Thêm mã sản phẩm vào danh sách quan tâm
     */
    public void addToWatchlist(String itemId) {
        if (!watchlist.contains(itemId)) {
            watchlist.add(itemId);
        }
    }

    public void addBalance(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    public boolean canAfford(double amount) {
        return this.balance >= amount;
    }

    @Override
    public UserRole getRole() {
        
        return UserRole.BIDDER;
    }

    @Override
    public String getDashboardView() {
        return "/views/bidder_dashboard.fxml";
    }
}