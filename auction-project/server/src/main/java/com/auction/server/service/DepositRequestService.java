package com.auction.server.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.dto.Dto;
import com.auction.model.Bidder;
import com.auction.model.User;

/**
 * ============================================================
 * DepositRequestService — Quản lý yêu cầu nạp tiền (cần Admin duyệt)
 * ============================================================
 *
 * Luồng nghiệp vụ:
 * 1. Bidder gửi REQUEST_DEPOSIT → tạo PendingDeposit, lưu in-memory
 * 2. Admin xem danh sách GET_PENDING_DEPOSITS → nhận list
 * 3. Admin APPROVE_DEPOSIT → gọi UserService.depositForBidder, xoá request
 * 4. Admin REJECT_DEPOSIT → xoá request, không nạp tiền
 *
 * (Dữ liệu in-memory — phù hợp cho demo; production nên lưu DB)
 * ============================================================
 */
public class DepositRequestService {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  /** Cấu trúc lưu một yêu cầu chờ duyệt */
  public record PendingDeposit(
      String requestId,
      String bidderId,
      String bidderName,
      String bidderEmail,
      double amount,
      LocalDateTime requestedAt) {
  }

  // requestId → PendingDeposit
  private final Map<String, PendingDeposit> pendingMap = new ConcurrentHashMap<>();

  private final UserService userService;

  public DepositRequestService(UserService userService) {
    this.userService = userService;
  }

  // -------------------------------------------------------
  // BIDDER: Gửi yêu cầu nạp tiền
  // -------------------------------------------------------

  /**
   * Bidder tạo yêu cầu nạp tiền, chờ Admin duyệt.
   *
   * @param bidderId ID Bidder
   * @param amount   Số tiền muốn nạp (phải > 0)
   * @return requestId của yêu cầu vừa tạo
   */
  public String requestDeposit(String bidderId, double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
    }

    User user = userService.findById(bidderId)
        .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản Bidder."));
    if (!(user instanceof Bidder bidder)) {
      throw new IllegalStateException("Chỉ Bidder mới có thể gửi yêu cầu nạp tiền.");
    }

    String requestId = UUID.randomUUID().toString();
    PendingDeposit pending = new PendingDeposit(
        requestId,
        bidderId,
        bidder.getFullName(),
        bidder.getEmail(),
        amount,
        LocalDateTime.now());
    pendingMap.put(requestId, pending);
    System.out.printf("[DepositRequestService] 📥 Yêu cầu nạp %.0f VND từ %s (requestId: %s)%n",
        amount, bidder.getFullName(), requestId.substring(0, 8));
    return requestId;
  }

  // -------------------------------------------------------
  // ADMIN: Xem / Duyệt / Từ chối
  // -------------------------------------------------------

  /**
   * Lấy toàn bộ danh sách yêu cầu đang chờ duyệt (sắp xếp mới nhất lên đầu).
   */
  public List<Dto.PendingDepositDto> getPendingDeposits() {
    return pendingMap.values().stream()
        .sorted((a, b) -> b.requestedAt().compareTo(a.requestedAt()))
        .map(p -> new Dto.PendingDepositDto(
            p.requestId(),
            p.bidderId(),
            p.bidderName(),
            p.bidderEmail(),
            p.amount(),
            p.requestedAt().format(FMT)))
        .toList();
  }

  /**
   * Admin duyệt yêu cầu → nạp tiền thực sự vào tài khoản Bidder.
   *
   * @param requestId ID yêu cầu
   * @return số dư mới của Bidder sau khi nạp
   */
  public double approveDeposit(String requestId) {
    PendingDeposit pending = pendingMap.remove(requestId);
    if (pending == null) {
      throw new IllegalArgumentException("Không tìm thấy yêu cầu nạp tiền (đã xử lý hoặc không tồn tại).");
    }
    userService.depositForBidder(pending.bidderId(), pending.amount());
    System.out.printf("[DepositRequestService] ✅ Đã duyệt nạp %.0f VND cho %s%n",
        pending.amount(), pending.bidderName());

    // Lấy số dư mới
    User updated = userService.findById(pending.bidderId()).orElseThrow();
    return ((Bidder) updated).getBalance();
  }

  /**
   * Admin từ chối yêu cầu → xoá khỏi danh sách, không nạp tiền.
   *
   * @param requestId ID yêu cầu
   * @param reason    Lý do từ chối
   * @return thông tin yêu cầu vừa bị từ chối (để log)
   */
  public PendingDeposit rejectDeposit(String requestId, String reason) {
    PendingDeposit pending = pendingMap.remove(requestId);
    if (pending == null) {
      throw new IllegalArgumentException("Không tìm thấy yêu cầu nạp tiền (đã xử lý hoặc không tồn tại).");
    }
    System.out.printf("[DepositRequestService] ❌ Từ chối nạp %.0f VND của %s. Lý do: %s%n",
        pending.amount(), pending.bidderName(), reason);
    return pending;
  }

  /**
   * Lấy thông tin một pending request theo ID (để gửi SYNC_BALANCE về Bidder sau
   * khi approve).
   */
  public String getBidderId(String requestId) {
    PendingDeposit p = pendingMap.get(requestId);
    return p != null ? p.bidderId() : null;
  }

  public PendingDeposit getPendingRequest(String requestId) {
    return pendingMap.get(requestId);
  }
}
