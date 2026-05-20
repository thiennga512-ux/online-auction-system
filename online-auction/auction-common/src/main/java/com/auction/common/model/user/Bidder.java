package com.auction.common.model.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * Class Bidder — Người tham gia đặt giá
 * ============================================================
 */
public class Bidder extends User {

    private double depositBalance;
    private double frozenBalance;
    private String shippingAddress;
    private int totalBidsPlaced;

    private final List<String> ongoingAuctions;
    private final List<String> watchlist;
    private final List<String> bidHistory;

    public Bidder(String fullName, String username, String email,
                  String passwordHash, String gender, String dateOfBirth) {
        super(fullName, username, email, passwordHash, gender, dateOfBirth);
        this.depositBalance = 0.0;
        this.frozenBalance  = 0.0;
        this.totalBidsPlaced = 0;
        this.shippingAddress = null;
        this.ongoingAuctions = new ArrayList<>();
        this.watchlist       = new ArrayList<>();
        this.bidHistory      = new ArrayList<>();
    }

    /** Constructor cho subclass (Seller) để chỉ định role đúng */
    protected Bidder(String fullName, String username, String email,
                  String passwordHash, String gender, String dateOfBirth, UserRole role) {
        super(fullName, username, email, passwordHash, gender, dateOfBirth, role);
        this.depositBalance = 0.0;
        this.frozenBalance  = 0.0;
        this.totalBidsPlaced = 0;
        this.shippingAddress = null;
        this.ongoingAuctions = new ArrayList<>();
        this.watchlist       = new ArrayList<>();
        this.bidHistory      = new ArrayList<>();
    }

    public Bidder(String id, String fullName, String username, String email,
                  String passwordHash, String phoneNumber, String gender,
                  String dateOfBirth, LocalDateTime createdAt, boolean active,
                  double depositBalance, int totalBidsPlaced, UserRole role) {
        super(id, fullName, username, email, passwordHash,
              phoneNumber, gender, dateOfBirth, createdAt, active, role);
        this.depositBalance  = depositBalance;
        this.totalBidsPlaced = totalBidsPlaced;
        this.frozenBalance   = 0.0;
        this.shippingAddress = null;
        this.ongoingAuctions = new ArrayList<>();
        this.watchlist       = new ArrayList<>();
        this.bidHistory      = new ArrayList<>();
    }

    public Bidder(String id, String fullName, String username, String email,
                  String passwordHash, String phoneNumber, String gender,
                  String dateOfBirth, LocalDateTime createdAt, boolean active,
                  double depositBalance, double frozenBalance, String shippingAddress, int totalBidsPlaced, UserRole role) {
        super(id, fullName, username, email, passwordHash,
              phoneNumber, gender, dateOfBirth, createdAt, active, role);
        this.depositBalance  = depositBalance;
        this.frozenBalance   = frozenBalance;
        this.shippingAddress = shippingAddress;
        this.totalBidsPlaced = totalBidsPlaced;
        this.ongoingAuctions = new ArrayList<>();
        this.watchlist       = new ArrayList<>();
        this.bidHistory      = new ArrayList<>();
    }


    @Override
    public String getDashboardView() {
        return "bidder-dashboard.fxml";
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        this.depositBalance += amount;
    }

    public boolean canAfford(double amount) {
        return this.depositBalance >= amount;
    }

    public double getDepositBalance() { return depositBalance; }
    public void setDepositBalance(double depositBalance) { this.depositBalance = depositBalance; }

    public double getFrozenBalance() { return frozenBalance; }
    public void setFrozenBalance(double frozenBalance) { this.frozenBalance = frozenBalance; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public int getTotalBidsPlaced() { return totalBidsPlaced; }
    public void setTotalBidsPlaced(int totalBidsPlaced) { this.totalBidsPlaced = totalBidsPlaced; }

    public void incrementBidsPlaced() { this.totalBidsPlaced++; }

    public void recordBidPlaced(String sessionId) {
        if (!ongoingAuctions.contains(sessionId)) {
            ongoingAuctions.add(sessionId);
        }
        bidHistory.add(sessionId);
        incrementBidsPlaced();
    }

    public void freezeBalance(double amount) {
        if (amount > depositBalance) {
            throw new IllegalArgumentException("Không đủ số dư để đóng băng");
        }
        depositBalance -= amount;
        frozenBalance += amount;
    }

    public void unfreezeBalance(double amount) {
        if (amount > frozenBalance) {
            throw new IllegalArgumentException("Số tiền giải phóng vượt quá số tiền đang đóng băng");
        }
        frozenBalance -= amount;
        depositBalance += amount;
    }

    public void deductFrozenBalance(double amount) {
        if (amount > frozenBalance) {
            throw new IllegalArgumentException("Số tiền trừ vượt quá số tiền đang đóng băng");
        }
        frozenBalance -= amount;
    }

    public List<String> getOngoingAuctions() { return ongoingAuctions; }
    public List<String> getWatchlist() { return watchlist; }
    public List<String> getBidHistory() { return bidHistory; }

    @Override
    public String toString() {
        return String.format(
            "[BIDDER] %s | Deposit: %.0f VND | Frozen: %.0f VND | Bids: %d",
            getFullName(), depositBalance, frozenBalance, totalBidsPlaced);
    }
}
