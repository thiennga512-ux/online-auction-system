package com.auction.server.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.network.Response;
import com.auction.server.observer.AuctionBroadcaster;
import com.auction.server.observer.ClientObserver;
import com.auction.service.auction.AutoBidConfig;
import com.auction.service.auction.Bid;
import com.auction.service.strategy.AggressiveBidStrategy;
import com.auction.service.strategy.AutoBidStrategy;
import com.auction.service.strategy.ConservativeBidStrategy;

public class AutoBidService implements ClientObserver {

  private final Map<String, List<AutoBidConfig>> autoBidConfigs = new ConcurrentHashMap<>();

  private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
    Thread t = new Thread(r, "AutoBid-Worker");
    t.setDaemon(true);
    return t;
  });

  private final AuctionService auctionService;
  private final UserService userService;

  public AutoBidService(
      AuctionService auctionService,
      UserService userService) {

    this.auctionService = auctionService;
    this.userService = userService;
  }

  public void registerAutoBid(AutoBidConfig config) {

    autoBidConfigs
        .computeIfAbsent(
            config.getSessionId(),
            k -> new CopyOnWriteArrayList<>())
        .add(config);

    AuctionBroadcaster
        .getInstance()
        .subscribe(config.getSessionId(), this);

    System.out.printf(
        "[AutoBidService] User %s đăng ký AutoBid (%s) cho phiên %s | Max: %,.0f%n",
        shortId(config.getBidderId()),
        config.getStrategyType(),
        shortId(config.getSessionId()),
        config.getMaxBid());
  }

  @Override
  public void onUpdate(Response response) {

    if (response == null) {
      return;
    }

    if (response.getActionType() != ActionType.NEW_BID_BROADCAST) {
      return;
    }

    try {

      Dto.NewBidEvent event = response.getDataAs(Dto.NewBidEvent.class);

      if (event == null) {
        System.out.println(
            "[AUTOBID] Parse NewBidEvent failed");
        return;
      }

      System.out.printf(
          "[AUTOBID] Session=%s | Bidder=%s | Price=%,.0f | Type=%s%n",
          shortId(event.sessionId()),
          shortId(event.bidderId()),
          event.amount(),
          event.bidType());

      handleNewBid(event);

    } catch (Exception e) {

      System.err.println(
          "[AUTOBID] onUpdate error: "
              + e.getMessage());

      e.printStackTrace();
    }
  }

  private void handleNewBid(Dto.NewBidEvent event) {

    String sessionId = event.sessionId();
    String leaderId = event.bidderId();

    List<AutoBidConfig> configs = autoBidConfigs.get(sessionId);

    if (configs == null || configs.isEmpty()) {

      System.out.println(
          "[AUTOBID] Không có config");

      return;
    }

    System.out.println(
        "[AUTOBID] Tìm thấy "
            + configs.size()
            + " config");

    double minIncrement = getMinIncrement(sessionId);

    executor.submit(() -> {

      for (AutoBidConfig config : configs) {

        // Không tự bid chính mình
        if (config.getBidderId().equals(leaderId)) {
          continue;
        }

        try {

          // Delay giả người thật
          Thread.sleep(
              1000 + (long) (Math.random() * 1500));

          processSingleAutoBid(
              config,
              minIncrement);

        } catch (InterruptedException e) {

          Thread.currentThread().interrupt();

        } catch (Exception e) {

          System.err.println(
              "[AUTOBID] process error: "
                  + e.getMessage());

          e.printStackTrace();
        }
      }
    });
  }

  private void processSingleAutoBid(
      AutoBidConfig config,
      double minIncrement) {

    // 1. Sửa đoạn này để lấy cả Session ra check
    var sessionOpt = auctionService.getSessionById(config.getSessionId());

    if (sessionOpt.isEmpty())
      return;
    var session = sessionOpt.get();

    // 2. CHECK QUAN TRỌNG: Nếu mình đang dẫn đầu thì dừng luôn
    // Lưu ý: Kiểm tra hàm lấy ID người thắng trong session của bạn tên là gì (ví dụ
    // getCurrentWinnerId)
    if (config.getBidderId().equals(session.getCurrentWinnerId())) {
      System.out.println("[AUTOBID] Bạn đang dẫn đầu phiên, không đặt thêm.");
      return;
    }

    double latestPrice = session.getCurrentPrice();

    System.out.printf(
        "[AUTOBID] Latest price: %,.0f%n",
        latestPrice);

    // --- Giữ nguyên đoạn dưới của bạn ---
    AutoBidStrategy strategy = switch (String.valueOf(config.getStrategyType())
        .toUpperCase()) {
      case "AGGRESSIVE" -> new AggressiveBidStrategy();
      default -> new ConservativeBidStrategy();
    };

    double nextBid = strategy.calculateNextBid(
        latestPrice,
        config.getMaxBid(),
        minIncrement);

    if (nextBid == -1) {
      System.out.println("[AUTOBID] Vượt max budget");
      removeConfig(config);
      return;
    }

    Optional<User> optionalUser = userService.findById(config.getBidderId());
    if (optionalUser.isEmpty()) {
      removeConfig(config);
      return;
    }

    User user = optionalUser.get();
    if (!(user instanceof Bidder bidder)) {
      removeConfig(config);
      return;
    }

    if (bidder.getBalance() < nextBid) {
      System.out.printf("[AUTOBID] %s không đủ tiền%n", bidder.getFullName());
      removeConfig(config);
      return;
    }

    // Thực hiện đặt giá...
    try {
      auctionService.placeBid(bidder, config.getSessionId(), nextBid, Bid.BidType.AUTO);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void removeConfig(AutoBidConfig config) {

    List<AutoBidConfig> configs = autoBidConfigs.get(config.getSessionId());

    if (configs == null) {
      return;
    }

    boolean removed = configs.remove(config);

    if (removed) {

      System.out.printf(
          "[AUTOBID] ❌ Remove config user %s%n",
          shortId(config.getBidderId()));
    }

    if (configs.isEmpty()) {

      autoBidConfigs.remove(
          config.getSessionId());
    }
  }

  private double getMinIncrement(String sessionId) {

    return auctionService
        .getSessionById(sessionId)
        .map(s -> s.getItem().getBidIncrement())
        .orElse(10000.0);
  }

  private String shortId(String id) {

    if (id == null) {
      return "null";
    }

    return id.length() <= 8
        ? id
        : id.substring(0, 8);
  }

  @Override
  public String getUserId() {

    return "AUTO_BID_SYSTEM";
  }
}