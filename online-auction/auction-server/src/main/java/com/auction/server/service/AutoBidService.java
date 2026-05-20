package com.auction.server.service;

import com.auction.common.dto.Dto;
import com.auction.common.model.auction.AutoBidConfig;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import com.auction.common.network.ActionType;
import com.auction.common.network.Response;
import com.auction.common.strategy.AggressiveBidStrategy;
import com.auction.common.strategy.AutoBidStrategy;
import com.auction.common.strategy.ConservativeBidStrategy;
import com.auction.server.observer.AuctionBroadcaster;
import com.auction.server.observer.ClientObserver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================
 * Class AutoBidService — Design Pattern: OBSERVER & STRATEGY
 * ============================================================
 * Lắng nghe tất cả biến động về giá, từ đó tự động phản kích lại giá
 * tùy theo "Chiến thuật" (Strategy) mà User đã cấu hình.
 * ============================================================
 */
public class AutoBidService implements ClientObserver {

  // Kịch bản: SessionId -> Danh sách cấu hình autobid của user trong phiên đó
  private final Map<String, List<AutoBidConfig>> autoBidConfigs = new ConcurrentHashMap<>();

  private final AuctionService auctionService;
  private final UserService userService;

  public AutoBidService(AuctionService auctionService, UserService userService) {
    this.auctionService = auctionService;
    this.userService = userService;
  }

  /**
   * User thêm cấu hình đấu giá tự động cho một phiên
   */
  public void registerAutoBid(AutoBidConfig config) {
    autoBidConfigs
        .computeIfAbsent(config.getSessionId(), k -> new CopyOnWriteArrayList<>())
        .add(config);

    // Đăng ký nhận thông báo của phiên đấu giá này (Observer Pattern)
    AuctionBroadcaster.getInstance().subscribe(config.getSessionId(), this);

    System.out.printf("[AutoBidService] User %s đăng ký Auto-Bid (%s) cho phiên %s. Max: %.0f%n",
        config.getBidderId().substring(0, 8), config.getStrategyType(),
        config.getSessionId().substring(0, 8), config.getMaxBudget());
  }

  /**
   * Kế thừa từ ClientObserver để lắng nghe toàn bộ các event.
   */
  @Override
  public void onUpdate(Response response) {
    // Chỉ quan tâm sự kiện có luồng đặt giá mới
    if (response.getActionType() == ActionType.NEW_BID_BROADCAST) {
      Dto.NewBidEvent event = response.getDataAs(Dto.NewBidEvent.class);
      if (event != null) {
        handleNewBid(event);
      }
    }
  }

  @Override
  public String getUserId() {
    return "AUTO_BID_BOT_SYSTEM";
  }

  /**
   * Xử lý luồng AutoBid khi có sự thay đổi giá
   */
  private void handleNewBid(Dto.NewBidEvent event) {
    String sessionId = event.sessionId();
    double currentPrice = event.amount();

    List<AutoBidConfig> configs = autoBidConfigs.get(sessionId);
    if (configs == null || configs.isEmpty()) {
      return;
    }

    // Chạy ngầm riêng (tránh block luồng broadcast)
    new Thread(() -> {
      for (AutoBidConfig config : configs) {
        // Tránh tự mình bid đè lên chính mình nếu vừa win giá
        // Ở thực tế ta cần có fullname/id từ event để so sánh.
        // Tạm thời giả định event.bidderName() có thể phân biệt.

        try {
          processSingleAutoBid(config, currentPrice);
        } catch (Exception e) {
          System.err.println("[AutoBidService] Lỗi chạy AutoBid: " + e.getMessage());
        }
      }
    }).start();
  }

  private void processSingleAutoBid(AutoBidConfig config, double currentPrice) {
    // 1. Áp dụng Strategy Pattern để chọn thuật toán
    AutoBidStrategy strategy = switch (config.getStrategyType().toUpperCase()) {
      case "AGGRESSIVE" -> new AggressiveBidStrategy();
      case "CONSERVATIVE" -> new ConservativeBidStrategy();
      default -> new ConservativeBidStrategy();
    };

    // Để tính chính xác, lấy minIncrement thông qua AuctionService hoặc lấy min
    // cứng
    // Tạm giả định minIncrement chung là 10_000 VND
    double minIncrement = 10000;

    // 2. Tính ra mức giá mới cần đặt
    double nextBid = strategy.calculateNextBid(currentPrice, config.getMaxBudget(), minIncrement);

    if (nextBid != -1) {
      // 3. User thực hiện Bid tự động
      User user = userService.findById(config.getBidderId()).orElse(null);
      if (user instanceof Bidder bidder) {
        // Thực thi đặt giá. Method placeBid tại AuctionService đã thread-safe.
        auctionService.placeBid(bidder, config.getSessionId(), nextBid);
        System.out.printf("[AutoBidService] 🤖 Bot tự động đặt %.0f cho %s%n", nextBid, bidder.getFullName());
      }
    } else {
      // Bot vượt quá ngân sách nên bị dội lại, có thể remove config ra khỏi map nếu
      // muốn
      // Tạm thời để im
    }
  }
}
