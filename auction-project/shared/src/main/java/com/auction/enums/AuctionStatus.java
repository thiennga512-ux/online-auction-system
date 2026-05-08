package com.auction.enums;

/**
 * Trạng thái phiên đấu giá: OPEN → RUNNING → FINISHED → PAID / CANCELED.
 */
public enum AuctionStatus {
    OPEN("Mở"),
    RUNNING("Đang diễn ra"),
    FINISHED("Kết thúc"),
    PAID("Đã thanh toán"),
    CANCELED("Đã hủy");

    private final String displayName;

    AuctionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
