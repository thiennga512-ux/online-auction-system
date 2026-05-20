package com.auction.common.model.auction;

import com.auction.common.model.item.Item;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * Class AuctionSession — Phiên đấu giá
 * ============================================================
 *
 * 🎓 GIẢI THÍCH ĐÂY LÀ CLASS TRUNG TÂM CỦA HỆ THỐNG:
 *
 * AuctionSession đại diện cho một "phòng đấu giá" đang diễn ra.
 * Nó kết nối tất cả các thành phần lại với nhau:
 *   - item     : sản phẩm đang được đấu giá
 *   - sellerId : người bán đã đăng phiên này
 *   - bids     : tất cả lượt đặt giá (lịch sử đầy đủ)
 *   - status   : trạng thái hiện tại (PENDING → OPEN → RUNNING → FINISHED)
 *
 * 🔑 CONCURRENCY (Giai đoạn 3):
 * Field currentPrice và currentWinnerId phải được đồng bộ hoá
 * khi nhiều Bidder cùng đặt giá. Chúng ta sẽ dùng:
 *   - synchronized methods
 *   - hoặc AtomicReference (sẽ nâng cấp ở Giai đoạn 3)
 *
 * 🔑 OBSERVER PATTERN (Giai đoạn 3):
 * Danh sách observers sẽ được thêm vào class này ở Giai đoạn 3.
 * Mỗi khi có bid mới, AuctionSession sẽ notify tất cả observers.
 *
 * 🔑 AUTO-BIDDING & ANTI-SNIPING (Giai đoạn 4):
 * - antiSnipingSeconds: nếu có bid trong X giây cuối → gia hạn thêm X giây
 * ============================================================
 */
public class AuctionSession {

  // -------------------------------------------------------
  // FIELDS
  // -------------------------------------------------------

  /** ID duy nhất của phiên đấu giá */
  private final String id;

  /** Tham chiếu đến sản phẩm đang được đấu giá */
  private final Item item;

  /** ID người bán tạo phiên này */
  private final String sellerId;

  /** Tên người bán (cache để giảm query) */
  private final String sellerName;

  /** Giá hiện tại cao nhất */
  private double currentPrice;

  /** ID Bidder đang dẫn đầu (null nếu chưa ai đặt) */
  private String currentWinnerId;

  /** Tên Bidder đang dẫn đầu (hiển thị UI) */
  private String currentWinnerName;

  /** Trạng thái phiên đấu giá */
  private AuctionStatus status;

  /** Thời điểm bắt đầu nhận bid */
  private LocalDateTime startTime;

  /** Thời điểm kết thúc ban đầu (có thể bị gia hạn bởi anti-sniping) */
  private LocalDateTime endTime;

  /** Thời điểm phiên thực sự kết thúc (sau tất cả gia hạn) */
  private LocalDateTime actualEndTime;

  /** Thời điểm phiên được tạo */
  private final LocalDateTime createdAt;

  /**
   * Số giây gia hạn khi có bid trong thời gian cuối (Anti-Sniping).
   * Mặc định 30 giây. 0 = tắt anti-sniping.
   */
  private int antiSnipingSeconds;

  /** Lịch sử tất cả các bid trong phiên này */
  private final List<Bid> bids;

  /** ID admin đã duyệt phiên (null nếu chưa duyệt) */
  private String approvedByAdminId;

  /** Ghi chú từ admin (khi từ chối) */
  private String adminNote;

  // -------------------------------------------------------
  // CONSTRUCTORS
  // -------------------------------------------------------

  /**
   * Constructor đầy đủ — tải từ database.
   */
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
    this.actualEndTime = actualEndTime != null ? actualEndTime : endTime;
    this.createdAt = createdAt;
    this.antiSnipingSeconds = antiSnipingSeconds;
    this.bids = new ArrayList<>();
    this.approvedByAdminId = approvedByAdminId;
    this.adminNote = adminNote;
  }

  /**
   * Constructor tạo phiên mới.
   * Phiên mới bắt đầu ở trạng thái PENDING (chờ Admin duyệt).
   *
   * @param item              sản phẩm cần đấu giá
   * @param sellerId          ID người bán
   * @param sellerName        tên người bán
   * @param startTime         thời gian bắt đầu nhận bid
   * @param endTime           thời gian kết thúc
   * @param antiSnipingSeconds số giây gia hạn anti-sniping (0 = tắt)
   */
  public AuctionSession(Item item, String sellerId, String sellerName,
      LocalDateTime startTime, LocalDateTime endTime, int antiSnipingSeconds) {
    this.id = UUID.randomUUID().toString();
    this.item = item;
    this.sellerId = sellerId;
    this.sellerName = sellerName;
    this.currentPrice = item.getBasePrice(); // Giá khởi điểm = basePrice của Item
    this.currentWinnerId = null;
    this.currentWinnerName = null;
    this.status = AuctionStatus.PENDING;     // Chờ Admin duyệt
    this.startTime = startTime;
    this.endTime = endTime;
    this.actualEndTime = endTime;
    this.createdAt = LocalDateTime.now();
    this.antiSnipingSeconds = antiSnipingSeconds;
    this.bids = new ArrayList<>();
    this.approvedByAdminId = null;
    this.adminNote = null;
  }

  // -------------------------------------------------------
  // BIDDING LOGIC — Sẽ nâng cấp synchronized ở Giai đoạn 3
  // -------------------------------------------------------

  /**
   * Xử lý một lượt đặt giá mới.
   *
   * 🎓 Luồng xử lý:
   *   1. Kiểm tra phiên có đang RUNNING không
   *   2. Kiểm tra giá bid có hợp lệ không (> currentPrice + minIncrement)
   *   3. Kiểm tra anti-sniping: nếu bid trong X giây cuối → gia hạn
   *   4. Cập nhật currentPrice và currentWinner
   *   5. Thêm bid vào lịch sử
   *
   * ⚠️ LƯU Ý GIAI ĐOẠN 3: Method này sẽ cần thêm "synchronized"
   * để tránh race condition khi nhiều Bidder cùng đặt giá.
   *
   * @param bid lượt đặt giá mới
   * @throws IllegalStateException    nếu phiên không ở trạng thái RUNNING
   * @throws IllegalArgumentException nếu giá không hợp lệ
   */
  public void placeBid(Bid bid) {
    // Bước 1: Kiểm tra trạng thái phiên
    if (!status.isAcceptingBids()) {
      throw new IllegalStateException(
          "Phiên đấu giá không nhận bid ở trạng thái: " + status.getDisplayName());
    }

    // Bước 2: Kiểm tra giá hợp lệ
    if (!item.isBidValid(currentPrice, bid.getAmount())) {
      throw new IllegalArgumentException(
          String.format("Giá đặt %.0f VND không hợp lệ. Tối thiểu: %.0f VND",
              bid.getAmount(), item.getNextMinimumBid(currentPrice)));
    }

    // Bước 3: Kiểm tra Anti-Sniping
    applyAntiSniping(bid.getTimestamp());

    // Bước 4: Cập nhật giá và người thắng hiện tại
    this.currentPrice = bid.getAmount();
    this.currentWinnerId = bid.getBidderId();
    this.currentWinnerName = bid.getBidderName();

    // Bước 5: Lưu vào lịch sử
    this.bids.add(bid);
  }

  /**
   * Áp dụng Anti-Sniping: gia hạn thêm thời gian nếu bid đặt trong X giây cuối.
   *
   * 🎓 Ví dụ (antiSnipingSeconds = 30):
   *   - Phiên kết thúc lúc 15:00:00
   *   - Bid đặt lúc 14:59:45 (còn 15 giây)
   *   → Gia hạn thêm 30 giây → kết thúc mới: 15:00:15
   *
   * @param bidTime thời điểm đặt giá
   */
  private void applyAntiSniping(LocalDateTime bidTime) {
    if (antiSnipingSeconds <= 0) return; // Anti-sniping bị tắt

    // Kiểm tra bid có trong khoảng X giây cuối không
    LocalDateTime snipingWindowStart = actualEndTime.minusSeconds(antiSnipingSeconds);
    if (!bidTime.isBefore(snipingWindowStart)) {
      // Bid trong vùng nguy hiểm → gia hạn thêm
      this.actualEndTime = this.actualEndTime.plusSeconds(antiSnipingSeconds);
    }
  }

  /**
   * Kết thúc phiên đấu giá — chuyển sang FINISHED.
   * Được gọi bởi server khi hết giờ.
   */
  public void finish() {
    if (status != AuctionStatus.RUNNING && status != AuctionStatus.OPEN) {
      throw new IllegalStateException("Không thể kết thúc phiên ở trạng thái: " + status);
    }
    this.status = AuctionStatus.FINISHED;
    this.actualEndTime = LocalDateTime.now();
  }

  /**
   * Huỷ phiên đấu giá.
   *
   * @param reason lý do huỷ (lưu vào adminNote)
   */
  public void cancel(String reason) {
    if (status.isTerminal()) {
      throw new IllegalStateException("Phiên đã kết thúc, không thể huỷ");
    }
    this.status = AuctionStatus.CANCELLED;
    this.adminNote = reason;
  }

  /**
   * Admin duyệt phiên đấu giá — chuyển từ PENDING sang OPEN.
   *
   * @param adminId ID admin duyệt
   */
  public void approve(String adminId) {
    if (status != AuctionStatus.PENDING) {
      throw new IllegalStateException("Chỉ phiên PENDING mới cần duyệt, hiện tại: " + status);
    }
    this.status = AuctionStatus.OPEN;
    this.approvedByAdminId = adminId;
  }

  /**
   * Bắt đầu phiên đấu giá — chuyển từ OPEN sang RUNNING.
   * Được gọi khi đến giờ startTime.
   */
  public void start() {
    if (status != AuctionStatus.OPEN) {
      throw new IllegalStateException("Chỉ phiên OPEN mới có thể bắt đầu, hiện tại: " + status);
    }
    this.status = AuctionStatus.RUNNING;
    // Tạo bid giá khởi điểm
    this.bids.add(Bid.createInitial(this.id, this.currentPrice));
  }

  // -------------------------------------------------------
  // COMPUTED PROPERTIES (thuộc tính tính toán)
  // -------------------------------------------------------

  /**
   * Kiểm tra xem phiên đã hết giờ chưa (dựa trên giờ thực).
   *
   * @return true nếu đã qua actualEndTime
   */
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(actualEndTime);
  }

  /**
   * Tính phí hoa hồng dựa trên danh mục sản phẩm.
   * Ví dụ: Electronics với giá 10tr → phí = 300.000 VND
   *
   * @return phí hoa hồng của phiên này
   */
  public double calculateCommissionFee() {
    return item.getCategoryFee(currentPrice);
  }

  /**
   * Lấy bid cao nhất trong phiên (thường là bid cuối cùng sau khi sort).
   *
   * @return bid thắng, hoặc null nếu chưa có bid nào từ người dùng
   */
  public Bid getWinningBid() {
    return bids.stream()
        .filter(b -> b.getBidType() != Bid.BidType.INITIAL)
        .max((b1, b2) -> {
          if (b1.getAmount() != b2.getAmount()) {
            return Double.compare(b1.getAmount(), b2.getAmount());
          }
          // Tie-break: bid sớm hơn thắng
          return b2.getTimestamp().compareTo(b1.getTimestamp());
        })
        .orElse(null);
  }

  /**
   * Có người thắng chưa? (Phiên kết thúc và có ít nhất 1 bid thật)
   *
   * @return true nếu phiên FINISHED và có winner
   */
  public boolean hasWinner() {
    return status == AuctionStatus.FINISHED && currentWinnerId != null;
  }

  // -------------------------------------------------------
  // GETTERS & SETTERS
  // -------------------------------------------------------

  public String getId() { return id; }
  public Item getItem() { return item; }
  public String getSellerId() { return sellerId; }
  public String getSellerName() { return sellerName; }
  public double getCurrentPrice() { return currentPrice; }
  public String getCurrentWinnerId() { return currentWinnerId; }
  public String getCurrentWinnerName() { return currentWinnerName; }
  public AuctionStatus getStatus() { return status; }
  public void setStatus(AuctionStatus status) { this.status = status; }
  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
  public LocalDateTime getActualEndTime() { return actualEndTime; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public int getAntiSnipingSeconds() { return antiSnipingSeconds; }
  public void setAntiSnipingSeconds(int seconds) { this.antiSnipingSeconds = seconds; }
  public String getApprovedByAdminId() { return approvedByAdminId; }
  public String getAdminNote() { return adminNote; }
  public void setAdminNote(String note) { this.adminNote = note; }

  /** @return Danh sách bid (chỉ đọc) */
  public List<Bid> getBids() {
    return Collections.unmodifiableList(bids);
  }

  /** Thêm nhiều bid khi tải từ database */
  public void loadBids(List<Bid> bidsFromDb) {
    this.bids.addAll(bidsFromDb);
  }

  @Override
  public String toString() {
    return String.format(
        "[AUCTION] %s | Sản phẩm: %s | Giá hiện tại: %.0f VND | Trạng thái: %s | Kết thúc: %s",
        id.substring(0, 8), item.getName(), currentPrice, status, actualEndTime);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AuctionSession other)) return false;
    return this.id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
