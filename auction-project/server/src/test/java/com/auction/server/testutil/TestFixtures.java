package com.auction.server.testutil;

import com.auction.enums.AuctionStatus;
import com.auction.enums.ItemCategory;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.server.service.PasswordHasher;
import com.auction.service.auction.AuctionSession;

import java.time.LocalDateTime;

public final class TestFixtures {

  public static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 25, 12, 0);
  public static final String PASSWORD = "secret123";
  public static final String PASSWORD_HASH = PasswordHasher.hash(PASSWORD);

  private TestFixtures() {}

  public static Seller seller(String id) {
    return new Seller(id, NOW, NOW, "seller1", PASSWORD_HASH,
        "seller@auction.vn", "Nguyễn Văn Bán", true,
        0, 0, 0, "Tech Store", "123456789");
  }

  public static Bidder bidder(String id, double balance) {
    Bidder b = new Bidder(id, NOW, NOW, "bidder1", PASSWORD_HASH,
        "bidder@auction.vn", "Trần Thị Mua", true, balance, 0, null);
    return b;
  }

  public static Admin admin(String id) {
    return new Admin(id, NOW, NOW, "admin", PASSWORD_HASH,
        "admin@auction.vn", "Admin Hệ Thống", true);
  }

  public static Item availableItem(String id, String sellerId) {
    Item item = new Item(id, NOW, NOW, "MacBook Pro", "Laptop cao cấp",
        1_000_000, 50_000, "img.png", sellerId, ItemCategory.ELECTRONICS);
    item.setAvailable(true);
    return item;
  }

  public static AuctionSession runningSession(String sessionId, Item item, String sellerId) {
    LocalDateTime start = NOW.minusHours(1);
    LocalDateTime end = NOW.plusHours(2);
    AuctionSession session = new AuctionSession(
        sessionId, item, sellerId, "Nguyễn Văn Bán",
        item.getStartingPrice(), null, null,
        AuctionStatus.RUNNING,
        start, end, end, NOW,
        0, null, null);
    return session;
  }

  public static AuctionSession pendingSession(String sessionId, Item item, String sellerId) {
    LocalDateTime start = NOW.plusHours(1);
    LocalDateTime end = NOW.plusHours(3);
    return new AuctionSession(
        sessionId, item, sellerId, "Nguyễn Văn Bán",
        item.getStartingPrice(), null, null,
        AuctionStatus.PENDING,
        start, end, end, NOW,
        30, null, null);
  }
}
