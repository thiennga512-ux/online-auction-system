package com.auction.server.testutil;

import com.auction.enums.AuctionStatus;
import com.auction.enums.ItemCategory;
import com.auction.enums.UserRole;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.service.auction.AuctionSession;

import java.time.LocalDateTime;

/** Dữ liệu mẫu dùng chung cho unit test server. */
public final class AuctionTestFixtures {

  private AuctionTestFixtures() {
  }

  public static Item sampleItem(String itemId, String sellerId) {
    return new Item(itemId, LocalDateTime.now(), LocalDateTime.now(),
        "Sản phẩm test", "Mô tả", 1_000_000, 100_000, "img.png", sellerId, ItemCategory.ART);
  }

  public static AuctionSession runningSession(String sessionId, Item item, String sellerId,
      double currentPrice, String winnerId) {
    LocalDateTime now = LocalDateTime.now();
    return new AuctionSession(
        sessionId, item, sellerId, "Seller Test",
        currentPrice, winnerId, winnerId != null ? "Winner" : null,
        AuctionStatus.RUNNING,
        now.minusHours(1), now.plusHours(2), now.plusHours(2), now.minusHours(2),
        0, null, null);
  }

  public static Bidder sampleBidder(String id, double balance, double frozenBalance) {
    return new Bidder(id, LocalDateTime.now(), LocalDateTime.now(),
        "bidder", "hash", "bidder@test.com", "Bidder Test", true,
        "Nam", "1990-01-01", balance, frozenBalance, "HN");
  }

  public static Seller sampleSeller(String id) {
    return new Seller(id, LocalDateTime.now(), LocalDateTime.now(),
        "seller", "hash", "seller@test.com", "Seller Test", true,
        "Nam", "1990-01-01", 0, 0, "HN", "Shop Test", "123456789");
  }

  public static Bidder inactiveBidder() {
    Bidder bidder = new Bidder("inactive", "hash", "inactive@test.com", "Inactive",
        "Nam", "1990-01-01");
    bidder.setActive(false);
    return bidder;
  }

  public static Bidder bidderWithHashedPassword(String rawPassword) {
    return new Bidder("user-1", LocalDateTime.now(), LocalDateTime.now(),
        "user", com.auction.server.service.PasswordHasher.hash(rawPassword),
        "user@test.com", "User Test", true, "Nam", "1990-01-01", 5_000_000, 0, "HN");
  }
}
