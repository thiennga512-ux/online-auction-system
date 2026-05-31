package com.auction.server.service;

import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.network.Response;
import com.auction.server.observer.AuctionBroadcaster;
import com.auction.server.observer.ClientObserver;
import com.auction.service.auction.AutoBidConfig;
import com.auction.service.auction.Bid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

public class AutoBidService implements ClientObserver {

  private final Map<String, PriorityBlockingQueue<AutoBidConfig>> autoBidConfigs = new ConcurrentHashMap<>();

  // Lock theo session để các luồng xử lý autobid của cùng 1 phiên không dẫm chân
  // lên nhau,
  // ngăn chặn spam đệ quy khi event NEW_BID liên tục bắn ra.
  private final Map<String, Object> sessionAutoBidLocks = new ConcurrentHashMap<>();

  // Lock theo user để đảm bảo nếu user tham gia nhiều autobid ở các phiên khác
  // nhau,
  // luồng sẽ không trừ tiền quá tay gây vượt số dư thực tế của user.
  private final Map<String, Object> userAutoBidLocks = new ConcurrentHashMap<>();

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

    PriorityBlockingQueue<AutoBidConfig> configs = autoBidConfigs
        .computeIfAbsent(
            config.getSessionId(),
            k -> new PriorityBlockingQueue<>(11, (c1, c2) -> {
              int priceCompare = Double.compare(c2.getMaxBid(), c1.getMaxBid());
              if (priceCompare != 0) {
                return priceCompare;
              }
              // Cùng maxBid: Ai đăng ký trước (createdAt nhỏ hơn) sẽ được ưu tiên xử lý trước
              return Long.compare(c1.getCreatedAt(), c2.getCreatedAt());
            }));

    // Remove old config for this bidder if any
    configs.removeIf(c -> c.getBidderId().equals(config.getBidderId()));

    if (config.getMaxBid() <= 0) {
      System.out.printf(
          "[AutoBidService] User %s huỷ AutoBid cho phiên %s%n",
          shortId(config.getBidderId()),
          shortId(config.getSessionId()));
      return;
    }

    configs.add(config);

    AuctionBroadcaster
        .getInstance()
        .subscribe(config.getSessionId(), this);

    System.out.printf(
        "[AutoBidService] User %s đăng ký AutoBid cho phiên %s | Max: %,.0f | Increment: %,.0f%n",
        shortId(config.getBidderId()),
        shortId(config.getSessionId()),
        config.getMaxBid(),
        config.getCustomIncrement());

    // Trigger immediate evaluation for the newly registered config
    evaluateAutoBids(config.getSessionId());
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
      System.out.printf("[AUTOBID] Session=%s | Bidder=%s | Price=%,.0f | Type=%s%n", shortId(event.sessionId()),
          shortId(event.bidderId()), event.amount(), event.bidType());

      handleNewBid(event);

    } catch (Exception e) {

      System.err.println(
          "[AUTOBID] onUpdate error: "
              + e.getMessage());

      e.printStackTrace();
    }
  }

  private void handleNewBid(Dto.NewBidEvent event) {
    evaluateAutoBids(event.sessionId());
  }

  private void evaluateAutoBids(String sessionId) {
    PriorityBlockingQueue<AutoBidConfig> configs = autoBidConfigs.get(sessionId);
    if (configs == null || configs.isEmpty()) {
      return;
    }

    executor.submit(() -> {
      Object sessionLock = sessionAutoBidLocks.computeIfAbsent(sessionId, k -> new Object());
      synchronized (sessionLock) {

        var sessionOpt = auctionService.getSessionById(sessionId);
        if (sessionOpt.isEmpty())
          return;
        var session = sessionOpt.get();

        double currentPrice = session.getCurrentPrice();
        String currentWinnerId = session.getCurrentWinnerId();
        double minSystemIncrement = getMinIncrement(sessionId);

        // Copy queue to a list to iterate in priority order
        List<AutoBidConfig> sortedConfigs = new ArrayList<>();
        PriorityBlockingQueue<AutoBidConfig> queueCopy = new PriorityBlockingQueue<>(configs);
        while (!queueCopy.isEmpty()) {
          sortedConfigs.add(queueCopy.poll());
        }

        List<AutoBidConfig> activeConfigs = new ArrayList<>();
        for (AutoBidConfig config : sortedConfigs) {
          // If the config belongs to the current winner, always consider it active
          if (config.getBidderId().equals(currentWinnerId)) {
            activeConfigs.add(config);
            continue;
          }

          // If their max bid is lower than the minimum required bid, they are out
          if (config.getMaxBid() < currentPrice + minSystemIncrement) {
            System.out.printf("[AUTOBID] Loại bỏ config của %s do maxBid < %,.0f%n", shortId(config.getBidderId()),
                currentPrice + minSystemIncrement);
            configs.remove(config);
            continue;
          }

          // Validate user exists and is a bidder
          Optional<User> optionalUser = userService.findById(config.getBidderId());
          if (optionalUser.isPresent() && optionalUser.get() instanceof Bidder bidder) {
            // Need at least enough balance to place one minimum bid
            if (bidder.getBalance() >= currentPrice + minSystemIncrement) {
              activeConfigs.add(config);
            } else {
              System.out.printf("[AUTOBID] Loại bỏ config của %s do không đủ số dư%n", shortId(config.getBidderId()));
              configs.remove(config);
            }
          } else {
            configs.remove(config);
          }
        }

        if (activeConfigs.isEmpty()) {
          return;
        }

        AutoBidConfig highestConfig = activeConfigs.get(0);
        AutoBidConfig secondHighestConfig = activeConfigs.size() > 1 ? activeConfigs.get(1) : null;

        double calculatedPrice = currentPrice;

        if (secondHighestConfig != null) {
          // Compare the top 2 autobids
          if (highestConfig.getMaxBid() == secondHighestConfig.getMaxBid()) {
            calculatedPrice = highestConfig.getMaxBid();
          } else {
            calculatedPrice = secondHighestConfig.getMaxBid() + highestConfig.getCustomIncrement();
            calculatedPrice = Math.min(calculatedPrice, highestConfig.getMaxBid());
          }
        } else {
          // Only 1 autobid vs manual bid
          if (!highestConfig.getBidderId().equals(currentWinnerId)) {
            calculatedPrice = currentPrice + highestConfig.getCustomIncrement();
            if (calculatedPrice < currentPrice + minSystemIncrement) {
              calculatedPrice = currentPrice + minSystemIncrement;
            }
            calculatedPrice = Math.min(calculatedPrice, highestConfig.getMaxBid());
          } else {
            return; // Already winning and no other autobids
          }
        }

        // Ensure calculated price meets the minimum increment requirement
        if (calculatedPrice < currentPrice + minSystemIncrement) {
          if (!highestConfig.getBidderId().equals(currentWinnerId)) {
            calculatedPrice = currentPrice + minSystemIncrement;
            if (calculatedPrice > highestConfig.getMaxBid()) {
              return; // Cannot bid, max bid exceeded
            }
          } else {
            // Already winning, no need to push price up if it doesn't meet increment
            return;
          }
        }

        if (calculatedPrice > currentPrice && calculatedPrice <= highestConfig.getMaxBid()) {
          Object userLock = userAutoBidLocks.computeIfAbsent(highestConfig.getBidderId(), k -> new Object());
          synchronized (userLock) {
            try {
              // Delay giả lập người thật
              Thread.sleep(1000 + (long) (Math.random() * 1500));

              Optional<User> optionalUser = userService.findById(highestConfig.getBidderId());
              if (optionalUser.isPresent() && optionalUser.get() instanceof Bidder bidder) {
                System.out.printf("[AUTOBID] Đặt giá %,.0f cho user %s tại phiên %s%n",
                    calculatedPrice, shortId(highestConfig.getBidderId()), shortId(sessionId));
                auctionService.placeBid(bidder, sessionId, calculatedPrice, Bid.BidType.AUTO);
                System.out.printf("[AUTOBID] Đặt giá thành công: %,.0f%n", calculatedPrice);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (Exception e) {
              System.err.println("Đặt giá thất bại: " + e.getMessage());

              configs.remove(highestConfig);
              evaluateAutoBids(sessionId);
            }
          }
        }
      }
    });
  }

  @SuppressWarnings("unused")
  private void removeConfig(AutoBidConfig config) {

    PriorityBlockingQueue<AutoBidConfig> configs = autoBidConfigs.get(config.getSessionId());

    if (configs == null) {
      return;
    }

    boolean removed = configs.remove(config);

    if (removed) {
      System.out.printf("[AUTOBID] Remove config user %s%n", shortId(config.getBidderId()));
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