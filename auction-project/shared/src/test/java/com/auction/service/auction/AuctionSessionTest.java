package com.auction.service.auction;

import com.auction.enums.AuctionStatus;
import com.auction.enums.ItemCategory;
import com.auction.model.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionSession — khởi tạo phiên đấu giá")
class AuctionSessionTest {

  private Item sampleItem() {
    return new Item("item-1", LocalDateTime.now(), LocalDateTime.now(),
        "Đồng hồ cổ", "Mô tả", 1_000_000, 100_000, "img.png", "seller-1", ItemCategory.ART);
  }

  @Test
  @DisplayName("constructor mới tạo phiên PENDING với giá khởi điểm từ item")
  void newSession_startsPendingWithStartingPrice() {
    Item item = sampleItem();
    LocalDateTime start = LocalDateTime.now().plusDays(1);
    LocalDateTime end = start.plusHours(3);

    AuctionSession session = new AuctionSession(item, "seller-1", "Shop ABC", start, end, 30);

    assertNotNull(session.getId());
    assertEquals(AuctionStatus.PENDING, session.getStatus());
    assertEquals(1_000_000, session.getCurrentPrice());
    assertNull(session.getCurrentWinnerId());
    assertEquals(start, session.getStartTime());
    assertEquals(end, session.getEndTime());
    assertEquals(end, session.getActualEndTime());
    assertEquals(30, session.getAntiSnipingSeconds());
    assertTrue(session.getBids().isEmpty());
  }

  @Test
  @DisplayName("getBids trả về danh sách không thể sửa đổi")
  void getBids_returnsUnmodifiableList() {
    AuctionSession session = new AuctionSession(
        sampleItem(), "seller-1", "Shop", LocalDateTime.now().plusDays(1),
        LocalDateTime.now().plusDays(1).plusHours(2), 0);

    assertThrows(UnsupportedOperationException.class,
        () -> session.getBids().add(Bid.createManual("s", "b", "A", 100)));
  }

  @Test
  @DisplayName("loadBids nạp lịch sử bid từ database")
  void loadBids_addsBidsFromDb() {
    AuctionSession session = new AuctionSession(
        sampleItem(), "seller-1", "Shop", LocalDateTime.now().plusDays(1),
        LocalDateTime.now().plusDays(1).plusHours(2), 0);
    Bid bid = Bid.createManual(session.getId(), "bidder-1", "A", 1_100_000);

    session.loadBids(java.util.List.of(bid));

    assertEquals(1, session.getBids().size());
    assertEquals(bid, session.getBids().get(0));
  }
}
