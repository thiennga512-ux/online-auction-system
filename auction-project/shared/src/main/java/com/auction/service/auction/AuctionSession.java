
package com.auction.service.auction;

import com.auction.model.*;
import com.auction.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AuctionSession {
    private final String id;
    private final Item item;
    private final String sellerId;
    private final String sellerName;

    private double currentPrice;
    private String currentWinnerId;
    private String currentWinnerName;

    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualEndTime;
    private final LocalDateTime createdAt;

    private int antiSnipingSeconds;
    private final List<Bid> bids;
    private String approvedByAdminId;
    private String adminNote;

    // Constructor nạp dữ liệu từ Database
    public AuctionSession(String id, Item item, String sellerId, String sellerName,
            double currentPrice, String currentWinnerId, String currentWinnerName,
            AuctionStatus status, LocalDateTime startTime, LocalDateTime endTime,
            LocalDateTime actualEndTime, LocalDateTime createdAt,
            int antiSnipingSeconds, String approvedByAdminId, String adminNote) {
        this.id = id;
        this.item = item;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
        this.currentWinnerName = currentWinnerName;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.actualEndTime = actualEndTime;
        this.createdAt = createdAt;
        this.antiSnipingSeconds = antiSnipingSeconds;
        this.approvedByAdminId = approvedByAdminId;
        this.adminNote = adminNote;
        this.bids = new ArrayList<>();
    }

    // Constructor tạo mới
    public AuctionSession(Item item, String sellerId, String sellerName,
            LocalDateTime startTime, LocalDateTime endTime, int antiSnipingSeconds) {
        this.id = UUID.randomUUID().toString();
        this.item = item;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.currentPrice = item.getStartingPrice();
        this.currentWinnerId = null;
        this.currentWinnerName = null;
        this.status = AuctionStatus.PENDING;
        this.startTime = startTime;
        this.endTime = endTime;
        this.actualEndTime = endTime;
        this.createdAt = LocalDateTime.now();
        this.antiSnipingSeconds = antiSnipingSeconds;
        this.bids = new ArrayList<>();
    }

    // ============================================================
    // FULL GETTERS
    // ============================================================

    public String getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getCurrentWinnerId() {
        return currentWinnerId;
    }

    public String getCurrentWinnerName() {
        return currentWinnerName;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getActualEndTime() {
        return actualEndTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getAntiSnipingSeconds() {
        return antiSnipingSeconds;
    }

    /**
     * Trả về danh sách không thể sửa đổi để bảo vệ dữ liệu nội bộ.
     */
    public List<Bid> getBids() {
        return Collections.unmodifiableList(bids);
    }

    public String getApprovedByAdminId() {
        return approvedByAdminId;
    }

    public String getAdminNote() {
        return adminNote;
    }

    // ============================================================
    // SETTERS (Chỉ cho các trường có thể thay đổi)
    // ============================================================

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setCurrentWinnerId(String currentWinnerId) {
        this.currentWinnerId = currentWinnerId;
    }

    public void setCurrentWinnerName(String currentWinnerName) {
        this.currentWinnerName = currentWinnerName;
    }

    public void setActualEndTime(LocalDateTime actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public void setApprovedByAdminId(String approvedByAdminId) {
        this.approvedByAdminId = approvedByAdminId;
    }

    public void loadBids(List<Bid> bidsFromDb) {
        this.bids.addAll(bidsFromDb);
    }
}