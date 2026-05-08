package com.auction.service.auction;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Class Bid đại diện cho một lượt đặt giá cụ thể trong hệ thống.
 * Thiết kế Immutable (Bất biến) để bảo vệ tính toàn vẹn của lịch sử đấu giá.
 */
public class Bid {

    private final String id;
    private final String auctionSessionId;
    private final String bidderId;
    private final String bidderName;
    private final double amount;
    private final LocalDateTime timestamp;
    private final BidType bidType;

    public enum BidType {
        MANUAL("Đặt thủ công"),
        INITIAL("Giá khởi điểm");

        private final String displayName;
        BidType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // Constructor dùng khi tải từ database
    public Bid(String id, String auctionSessionId, String bidderId, String bidderName,
               double amount, LocalDateTime timestamp, BidType bidType) {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền phải > 0");
        this.id = id;
        this.auctionSessionId = auctionSessionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = timestamp;
        this.bidType = bidType;
    }

    // Factory method tạo Bid đặt tay mới
    public static Bid createManual(String auctionSessionId, String bidderId, String bidderName, double amount) {
        return new Bid(
            UUID.randomUUID().toString(),
            auctionSessionId,
            bidderId,
            bidderName,
            amount,
            LocalDateTime.now(),
            BidType.MANUAL
        );
    }

    // Factory method tạo Bid khởi điểm (khi phiên vừa mở)
    public static Bid createInitial(String auctionSessionId, double basePrice) {
        return new Bid(
            UUID.randomUUID().toString(),
            auctionSessionId,
            null, 
            "Hệ thống",
            basePrice,
            LocalDateTime.now(),
            BidType.INITIAL
        );
    }

    // Getters
    public String getId() { return id; }
    public String getAuctionSessionId() { return auctionSessionId; }
    public String getBidderId() { return bidderId; }
    public String getBidderName() { return bidderName; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public BidType getBidType() { return bidType; }

    @Override
    public String toString() {
        return String.format("[%s] %s: %.0f VND (%s)", 
            bidType.getDisplayName(), bidderName, amount, timestamp);
    }
}