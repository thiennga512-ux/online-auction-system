package com.auction.service.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bid — logic đặt giá")
class BidTest {

  private Bid manualBid(String id, String bidderId, double amount, LocalDateTime timestamp) {
    return new Bid(id, "session-1", bidderId, "Người đấu giá", amount, null, timestamp, Bid.BidType.MANUAL);
  }

  @Test
  @DisplayName("createManual tạo bid với loại MANUAL và amount hợp lệ")
  void createManual_setsCorrectTypeAndAmount() {
    Bid bid = Bid.createManual("session-1", "bidder-1", "Nguyễn A", 5_000_000);

    assertEquals(Bid.BidType.MANUAL, bid.getBidType());
    assertEquals(5_000_000, bid.getAmount());
    assertEquals("bidder-1", bid.getBidderId());
    assertNull(bid.getMaxAutoBid());
    assertFalse(bid.isAutoBid());
  }

  @Test
  @DisplayName("createAuto lưu maxAutoBid và loại AUTO")
  void createAuto_setsMaxAutoBid() {
    Bid bid = Bid.createAuto("session-1", "bidder-1", "Nguyễn A", 3_000_000, 10_000_000);

    assertEquals(Bid.BidType.AUTO, bid.getBidType());
    assertEquals(10_000_000, bid.getMaxAutoBid());
    assertTrue(bid.isAutoBid());
  }

  @Test
  @DisplayName("amount <= 0 ném IllegalArgumentException")
  void invalidAmount_throwsException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Bid("id", "session", "b1", "A", 0, null, LocalDateTime.now(), Bid.BidType.MANUAL));
    assertThrows(IllegalArgumentException.class,
        () -> new Bid("id", "session", "b1", "A", -100, null, LocalDateTime.now(), Bid.BidType.MANUAL));
  }

  @Test
  @DisplayName("winsOver: giá cao hơn luôn thắng")
  void winsOver_higherAmountWins() {
    LocalDateTime t = LocalDateTime.of(2025, 5, 1, 10, 0);
    Bid higher = manualBid("b1", "u1", 2_000_000, t);
    Bid lower = manualBid("b2", "u2", 1_500_000, t.plusMinutes(5));

    assertTrue(higher.winsOver(lower));
    assertFalse(lower.winsOver(higher));
  }

  @Test
  @DisplayName("winsOver: cùng giá thì người đặt trước thắng (tie-break)")
  void winsOver_sameAmountEarlierTimestampWins() {
    Bid earlier = manualBid("b1", "u1", 1_000_000, LocalDateTime.of(2025, 5, 1, 10, 0));
    Bid later = manualBid("b2", "u2", 1_000_000, LocalDateTime.of(2025, 5, 1, 10, 30));

    assertTrue(earlier.winsOver(later));
    assertFalse(later.winsOver(earlier));
  }

  @Test
  @DisplayName("equals dựa trên id")
  void equals_basedOnId() {
    LocalDateTime t = LocalDateTime.now();
    Bid bid1 = manualBid("same-id", "u1", 100, t);
    Bid bid2 = manualBid("same-id", "u2", 200, t);

    assertEquals(bid1, bid2);
  }
}
