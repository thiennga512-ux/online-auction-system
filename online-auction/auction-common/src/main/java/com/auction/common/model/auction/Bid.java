package com.auction.common.model.auction;

import java.time.LocalDateTime;
import java.util.UUID;


public class Bid {
  private final String id;

  /** ID phiên đấu giá mà bid này thuộc về */
  private final String auctionSessionId;

  /** ID của Bidder đặt giá */
  private final String bidderId;

  /** Tên hiển thị của Bidder (lưu cache để tránh query lại) */
  private final String bidderName;

  /**
   * Số tiền đặt giá (VND).
   * Đây là giá ĐỀ XUẤT — chỉ hợp lệ khi > currentPrice + minIncrement.
   */
  private final double amount;

  /**
   * Giá tối đa cho phép khi dùng Auto-Bidding (Giai đoạn 4).
   * null = đặt thủ công, không dùng auto-bidding.
   * Ví dụ: maxAutoBid = 5.000.000 → system tự tăng giá đến tối đa 5tr.
   */
  private final Double maxAutoBid;

  /**
   * Thời điểm đặt giá — quan trọng để giải quyết tie-break
   * (khi 2 người đặt cùng giá, người đặt TRƯỚC thắng).
   */
  private final LocalDateTime timestamp;

  /**
   * Loại bid:
   * - MANUAL  : do người dùng tự bấm
   * - AUTO    : do hệ thống auto-bid tự đặt
   * - INITIAL : giá khởi điểm (do system tạo khi bắt đầu phiên)
   */
  public enum BidType {
    MANUAL("Đặt thủ công"),
    AUTO("Auto-Bidding"),
    INITIAL("Giá khởi điểm");

    private final String displayName;
    BidType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
  }

  private final BidType bidType;

  // -------------------------------------------------------
  // CONSTRUCTORS
  // -------------------------------------------------------

  /**
   * Constructor đầy đủ — dùng khi tải từ database.
   */
  public Bid(String id, String auctionSessionId, String bidderId, String bidderName,
      double amount, Double maxAutoBid, LocalDateTime timestamp, BidType bidType) {
    validateAmount(amount);
    this.id = id;
    this.auctionSessionId = auctionSessionId;
    this.bidderId = bidderId;
    this.bidderName = bidderName;
    this.amount = amount;
    this.maxAutoBid = maxAutoBid;
    this.timestamp = timestamp;
    this.bidType = bidType;
  }

  /**
   * Factory method — tạo Bid thủ công mới.
   * Dùng static method thay vì constructor: code đọc dễ hiểu hơn.
   *
   * Ví dụ: Bid.createManual(sessionId, bidderId, "Nguyễn A", 5_000_000)
   */
  public static Bid createManual(String auctionSessionId, String bidderId,
      String bidderName, double amount) {
    return new Bid(
        UUID.randomUUID().toString(),
        auctionSessionId,
        bidderId,
        bidderName,
        amount,
        null,           // Không có maxAutoBid khi đặt thủ công
        LocalDateTime.now(),
        BidType.MANUAL
    );
  }

  /**
   * Factory method — tạo Bid Auto-Bidding.
   *
   * @param maxAutoBid giới hạn tối đa mà system được phép đặt thay
   */
  public static Bid createAuto(String auctionSessionId, String bidderId,
      String bidderName, double amount, double maxAutoBid) {
    return new Bid(
        UUID.randomUUID().toString(),
        auctionSessionId,
        bidderId,
        bidderName,
        amount,
        maxAutoBid,
        LocalDateTime.now(),
        BidType.AUTO
    );
  }

  /**
   * Factory method — tạo Bid giá khởi điểm (do system tạo khi bắt đầu phiên).
   */
  public static Bid createInitial(String auctionSessionId, double basePrice) {
    return new Bid(
        UUID.randomUUID().toString(),
        auctionSessionId,
        null, // null bidder_id cho hệ thống để tránh lỗi khoá ngoại
        "Hệ thống",
        basePrice,
        null,
        LocalDateTime.now(),
        BidType.INITIAL
    );
  }

  // -------------------------------------------------------
  // VALIDATION HELPER
  // -------------------------------------------------------

  private void validateAmount(double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Số tiền đặt giá phải > 0, nhận: " + amount);
    }
  }

  // -------------------------------------------------------
  // BUSINESS LOGIC
  // -------------------------------------------------------

  /**
   * So sánh 2 bid để xác định bid nào "thắng".
   * Bid thắng = giá cao hơn; nếu bằng nhau → bid đặt SỚM hơn thắng.
   *
   * @param other bid cần so sánh
   * @return true nếu bid NÀY thắng bid other
   */
  public boolean winsOver(Bid other) {
    if (this.amount > other.amount) return true;
    if (this.amount == other.amount) {
      // Tie-break: timestamp sớm hơn thắng
      return this.timestamp.isBefore(other.timestamp);
    }
    return false;
  }

  // -------------------------------------------------------
  // GETTERS ONLY (Immutable — không có setters)
  // -------------------------------------------------------

  public String getId() { return id; }
  public String getAuctionSessionId() { return auctionSessionId; }
  public String getBidderId() { return bidderId; }
  public String getBidderName() { return bidderName; }
  public double getAmount() { return amount; }
  public Double getMaxAutoBid() { return maxAutoBid; }
  public LocalDateTime getTimestamp() { return timestamp; }
  public BidType getBidType() { return bidType; }
  public boolean isAutoBid() { return bidType == BidType.AUTO; }

  @Override
  public String toString() {
    return String.format("[BID] %s đặt %.0f VND | Lúc: %s | Loại: %s",
        bidderName, amount, timestamp.toString(), bidType.getDisplayName());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Bid other)) return false;
    return this.id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
