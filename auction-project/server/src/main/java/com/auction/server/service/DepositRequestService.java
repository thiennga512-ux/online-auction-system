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

public class DepositRequestService {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  public record PendingDeposit(
      String requestId,
      String bidderId,
      String bidderName,
      String bidderEmail,
      double amount,
      LocalDateTime requestedAt) {
  }

  private final Map<String, PendingDeposit> pendingMap = new ConcurrentHashMap<>();

  private final UserService userService;

  public DepositRequestService(UserService userService) {
    this.userService = userService;
  }

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
    System.out.printf("[DepositRequestService] Yêu cầu nạp %.0f VND từ %s (requestId: %s)%n",
        amount, bidder.getFullName(), requestId.substring(0, 8));
    return requestId;
  }

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

  public double approveDeposit(String requestId) {
    PendingDeposit pending = pendingMap.remove(requestId);
    if (pending == null) {
      throw new IllegalArgumentException("Không tìm thấy yêu cầu nạp tiền (đã xử lý hoặc không tồn tại).");
    }
    userService.depositForBidder(pending.bidderId(), pending.amount());
    System.out.printf("[DepositRequestService] Đã duyệt nạp %.0f VND cho %s%n",
        pending.amount(), pending.bidderName());

    User updated = userService.findById(pending.bidderId()).orElseThrow();
    return ((Bidder) updated).getBalance();
  }


  public PendingDeposit rejectDeposit(String requestId, String reason) {
    PendingDeposit pending = pendingMap.remove(requestId);
    if (pending == null) {
      throw new IllegalArgumentException("Không tìm thấy yêu cầu nạp tiền (đã xử lý hoặc không tồn tại).");
    }
    System.out.printf("[DepositRequestService] Từ chối nạp %.0f VND của %s. Lý do: %s%n",
        pending.amount(), pending.bidderName(), reason);
    return pending;
  }

  public String getBidderId(String requestId) {
    PendingDeposit p = pendingMap.get(requestId);
    return p != null ? p.bidderId() : null;
  }

  public PendingDeposit getPendingRequest(String requestId) {
    return pendingMap.get(requestId);
  }
}
