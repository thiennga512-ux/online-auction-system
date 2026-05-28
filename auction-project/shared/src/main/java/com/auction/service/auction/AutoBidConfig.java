package com.auction.service.auction;

import java.io.Serializable;

/**
 * ============================================================
 * Class AutoBidConfig — Cấu hình Auto-bidding của một User
 * ============================================================
 * Lưu trữ thiết lập tự động đấu giá của một Bidder cho một phiên cụ thể.
 * Được truyền qua mạng nên cần implements Serializable.
 * ============================================================
 */
public class AutoBidConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String bidderId;
    private final double maxBid;
    private final double customIncrement;
    private final long createdAt; // Thời gian đăng ký autobid

    public AutoBidConfig(String sessionId, String bidderId, double maxBid, double customIncrement) {
        this.sessionId = sessionId;
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.customIncrement = customIncrement;
        this.createdAt = System.nanoTime(); // Sử dụng System.nanoTime() để có độ chính xác cao nhất
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public double getCustomIncrement() {
        return customIncrement;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
