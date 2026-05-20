package com.auction.common.model.auction;

/**
 * ============================================================
 * Enum AuctionStatus — Trạng thái của phiên đấu giá
 * ============================================================
 *
 * 🎓 GIẢI THÍCH:
 * Một phiên đấu giá trải qua các giai đoạn theo thứ tự:
 *
 *     PENDING → OPEN → RUNNING → FINISHED
 *                          ↘ CANCELLED (nếu bị huỷ)
 *
 * - PENDING  : Đã tạo nhưng chưa được Admin duyệt
 * - OPEN     : Đã duyệt, mở đăng ký tham gia (chưa nhận bid)
 * - RUNNING  : Đang diễn ra, nhận bid từ Bidder
 * - FINISHED : Kết thúc tự nhiên (hết giờ)
 * - CANCELLED: Bị huỷ bởi Admin hoặc Seller
 *
 * Việc dùng Enum giúp kiểm soát state machine rõ ràng:
 *   if (session.getStatus() == AuctionStatus.RUNNING) { ... }
 * ============================================================
 */
public enum AuctionStatus {

  PENDING("Chờ duyệt", "⏳"),
  OPEN("Đang mở đăng ký", "🟢"),
  RUNNING("Đang đấu giá", "🔴"),
  FINISHED("Đã kết thúc", "✅"),
  CANCELLED("Đã huỷ", "❌");

  private final String displayName;
  private final String icon; // Icon emoji hiển thị trên UI

  AuctionStatus(String displayName, String icon) {
    this.displayName = displayName;
    this.icon = icon;
  }

  public String getDisplayName() { return displayName; }
  public String getIcon() { return icon; }

  /**
   * Kiểm tra xem phiên đấu giá có thể nhận bid hay không.
   * Chỉ trạng thái RUNNING mới chấp nhận bid.
   *
   * @return true nếu có thể đặt giá
   */
  public boolean isAcceptingBids() {
    return this == RUNNING;
  }

  /**
   * Kiểm tra xem phiên đấu giá đã kết thúc (không thể thay đổi).
   *
   * @return true nếu đã kết thúc hoặc bị huỷ
   */
  public boolean isTerminal() {
    return this == FINISHED || this == CANCELLED;
  }

  /** Chuyển String → AuctionStatus */
  public static AuctionStatus fromString(String value) {
    if (value == null) {
      throw new IllegalArgumentException("AuctionStatus không được null");
    }
    for (AuctionStatus status : values()) {
      if (status.name().equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Không tìm thấy AuctionStatus: " + value);
  }

  @Override
  public String toString() {
    return icon + " " + displayName;
  }
}
