package com.auction.server.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.auction.enums.AuctionStatus;
import com.auction.server.service.AuctionService;
import com.auction.service.auction.AuctionSession;

public class AuctionTimerManager {

  // SINGLETON: Instance duy nhất
  private static volatile AuctionTimerManager instance;

  // Dùng để chạy lịch trình định kỳ
  private final ScheduledExecutorService scheduler;
  private AuctionService auctionService;

  // Private constructor
  private AuctionTimerManager() {
    // Chỉ cần 1 thread ngầm chạy là đủ cho Timer
    this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread t = new Thread(runnable);
      t.setDaemon(true); // Để không chặn server tắt
      t.setName("AuctionTimer-Thread");
      return t;
    });
  }

  // Lấy instance duy nhất (Thread-safe)
  public static AuctionTimerManager getInstance() {
    if (instance == null) {
      synchronized (AuctionTimerManager.class) {
        if (instance == null) {
          instance = new AuctionTimerManager();
        }
      }
    }
    return instance;
  }

  // Cần truyền AuctionService vào trước khi bắt đầu
  public void initialize(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  /**
   * Bắt đầu chạy vòng lặp mỗi N giây quét DB.
   */
  public void start() {
    if (this.auctionService == null) {
      throw new IllegalStateException("Cần initialize(AuctionService) trước khi start!");
    }

    // Quét mỗi 10 giây
    scheduler.scheduleAtFixedRate(this::scanAndProcessAuctions, 0, 10, TimeUnit.SECONDS);
    System.out.println("[AuctionTimerManager] Đã khởi động Timer background.");
  }

  public void stop() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
      System.out.println("[AuctionTimerManager] Đã dừng Timer.");
    }
  }

  /**
   * Quét toàn bộ phiên đấu giá và cập nhật trạng thái tự động.
   */
  private void scanAndProcessAuctions() {
    try {
      LocalDateTime now = LocalDateTime.now();

      // 1. Quét các phiên OPEN xem có cái nào đến giờ bắt đầu chưa
      List<AuctionSession> openSessions = auctionService.getAuctionsByStatus(AuctionStatus.OPEN);
      for (AuctionSession session : openSessions) {
        if (!now.isBefore(session.getStartTime())) { // now >= startTime
          auctionService.startAuction(session.getId());
        }
      }

      // 2. Quét các phiên RUNNING xem có cái nào đến giờ kết thúc chưa
      List<AuctionSession> runningSessions = auctionService.getAuctionsByStatus(AuctionStatus.RUNNING);
      for (AuctionSession session : runningSessions) {
        if (!now.isBefore(session.getActualEndTime())) { // now >= actualEndTime
          auctionService.finishAuction(session.getId());
        }
      }

    } catch (Exception e) {
      System.err.println("[AuctionTimerManager] Lỗi khi quét phiên đấu giá: " + e.getMessage());
      e.printStackTrace();
    }
  }
}

