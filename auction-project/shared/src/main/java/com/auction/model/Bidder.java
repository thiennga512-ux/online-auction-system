package com.auction.model;

<<<<<<< HEAD
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
=======
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
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
        this.balance = balance;
        this.frozenBalance = frozenBalance;
        this.shippingAddress = shippingAddress;
        
        // Các danh sách này thường sẽ được nạp riêng thông qua Service/Repository
        this.ongoingAuctions = new ArrayList<>();
        this.watchlist = new ArrayList<>();
        this.bidHistory = new ArrayList<>();
    }
<<<<<<< HEAD
    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
=======

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
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
    }

    @Override
    public String getDashboardView() {
        return "/views/bidder_dashboard.fxml";
    }
<<<<<<< HEAD
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
=======

    public void setBalance(double balance) {
        this.balance = balance;
    }
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
}