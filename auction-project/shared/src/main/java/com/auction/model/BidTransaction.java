package com.auction.model;
import java.time.LocalDateTime;
//Ghi lai moi lan dat gia
public class BidTransaction extends BaseEntity {

    private final long auctionId;
    private final long bidderId;
    private final double amount;
    private LocalDateTime bidTime;
    private boolean isWinning; // true nếu đây là bid cao nhất hiện tại

    public BidTransaction(long auctionId, long bidderId, double amount) {
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidTime = LocalDateTime.now();
        this.isWinning = false;
    }

    @Override
    public void printInfo() {
        System.out.printf("[BidTransaction] ID=%d | Auction=%d | Bidder=%d | Giá=%.2f | Thời gian=%s | %s%n",
                getId(), auctionId, bidderId, amount, bidTime,
                isWinning ? "★ Đang dẫn đầu" : "");
    }

    public long getAuctionId() { return auctionId; }
    public long getBidderId() { return bidderId; }
    public double getAmount() { return amount; }
    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    public boolean isWinning() { return isWinning; }
    public void setWinning(boolean winning) { this.isWinning = winning; }
}
