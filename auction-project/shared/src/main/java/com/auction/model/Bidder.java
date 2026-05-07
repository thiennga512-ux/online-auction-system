package com.auction.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.enums.UserRole;
import model.auction.AuctionSession; // Giả sử bạn đã có class này
import model.auction.BidHistory;    // Giả sử bạn đã có class này

public class Bidder extends User {
    // 1. Thuộc tính tài chính
    private double balance;         // Số dư khả dụng
    private double frozenBalance;   // Số dư bị tạm khóa khi đang dẫn đầu đấu giá
    private String shippingAddress;

    // 2. Thuộc tính danh sách (Chỉ chứa dữ liệu trên RAM)
    private List<AuctionSession> ongoingAuctions; // Các phiên đang tham gia
    private List<AuctionSession> watchlist;       // Danh sách quan tâm
    private List<BidHistory> bidHistory;          // Lịch sử thắng/thua

    // Constructor dùng cho Đăng ký mới (BaseEntity sẽ tự tạo UUID)
    public Bidder(String username, String passwordHash, String email, String fullName) {
        super(username, passwordHash, email, fullName, UserRole.BIDDER);
        this.balance = 0.0;
        this.frozenBalance = 0.0;
        this.ongoingAuctions = new ArrayList<>();
        this.watchlist = new ArrayList<>();
        this.bidHistory = new ArrayList<>();
    }

    // Constructor dùng để nạp dữ liệu từ MySQL (Cần đầy đủ thông tin từ DB)
    public Bidder(String id, LocalDateTime createdAt, LocalDateTime updateAt, String username, 
                  String passwordHash, String email, String fullName, boolean active, 
                  double balance, double frozenBalance, String shippingAddress) {
        super(id, createdAt, updateAt, username, passwordHash, email, fullName, UserRole.BIDDER, active);
        this.balance = balance;
        this.frozenBalance = frozenBalance;
        this.shippingAddress = shippingAddress;
        
        // Các danh sách này thường sẽ được nạp riêng thông qua Service/Repository
        this.ongoingAuctions = new ArrayList<>();
        this.watchlist = new ArrayList<>();
        this.bidHistory = new ArrayList<>();
    }
    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
    }

    @Override
    public String getDashboardView() {
        return "/views/bidder_dashboard.fxml";
    }
    public void addBalance(double amount) {
        if (amount > 0) this.balance += amount;
    }

    public boolean canAfford(double amount) {
        return this.balance >= amount;
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getFrozenBalance() { return frozenBalance; }
    public void setFrozenBalance(double frozenBalance) { this.frozenBalance = frozenBalance; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public List<AuctionSession> getOngoingAuctions() { return ongoingAuctions; }
    public List<AuctionSession> getWatchlist() { return watchlist; }
    public List<BidHistory> getBidHistory() { return bidHistory; }
}