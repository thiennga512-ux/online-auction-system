package com.auction.server.service;

import com.auction.enums.AuctionStatus;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.testutil.AuctionTestFixtures;
import com.auction.service.auction.AuctionSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionCreationService — tạo phiên đấu giá")
class AuctionCreationServiceTest {

  private static final String ITEM_ID = "item-1";
  private static final String SELLER_ID = "seller-1";

  @Mock
  private AuctionSessionDAO auctionSessionDAO;
  @Mock
  private ItemService itemService;

  private AuctionCreationService creationService;
  private Seller seller;
  private Item item;

  @BeforeEach
  void setUp() {
    creationService = new AuctionCreationService(auctionSessionDAO, itemService);
    seller = AuctionTestFixtures.sampleSeller(SELLER_ID);
    item = AuctionTestFixtures.sampleItem(ITEM_ID, SELLER_ID);
  }

  private LocalDateTime validStart() {
    return LocalDateTime.now().plusMinutes(10);
  }

  private LocalDateTime validEnd(LocalDateTime start) {
    return start.plusHours(2);
  }

  @Test
  @DisplayName("tạo phiên thành công — PENDING và đánh dấu item không còn available")
  void createAuction_success() throws Exception {
    LocalDateTime start = validStart();
    LocalDateTime end = validEnd(start);
    when(itemService.findById(ITEM_ID)).thenReturn(Optional.of(item));

    AuctionSession session = creationService.createAuction(seller, ITEM_ID, start, end, 60);

    assertEquals(AuctionStatus.PENDING, session.getStatus());
    assertEquals(item.getStartingPrice(), session.getCurrentPrice());
    assertEquals(60, session.getAntiSnipingSeconds());
    assertFalse(item.isAvailable());

    verify(auctionSessionDAO).save(session);
    verify(itemService).markItemAsSold(ITEM_ID);
  }

  @Test
  @DisplayName("thời gian bắt đầu quá sớm (< 5 phút) bị từ chối")
  void createAuction_rejectsStartTooSoon() {
    LocalDateTime start = LocalDateTime.now().plusMinutes(2);
    LocalDateTime end = start.plusHours(2);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> creationService.createAuction(seller, ITEM_ID, start, end, 0));

    assertTrue(ex.getMessage().contains("5 phút"));
    verifyNoInteractions(auctionSessionDAO);
  }

  @Test
  @DisplayName("phiên ngắn hơn 1 giờ bị từ chối")
  void createAuction_rejectsDurationTooShort() {
    LocalDateTime start = validStart();
    LocalDateTime end = start.plusMinutes(30);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> creationService.createAuction(seller, ITEM_ID, start, end, 0));

    assertTrue(ex.getMessage().contains("1 giờ"));
  }

  @Test
  @DisplayName("phiên dài hơn 30 ngày bị từ chối")
  void createAuction_rejectsDurationTooLong() {
    LocalDateTime start = validStart();
    LocalDateTime end = start.plusDays(31);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> creationService.createAuction(seller, ITEM_ID, start, end, 0));

    assertTrue(ex.getMessage().contains("30 ngày"));
  }

  @Test
  @DisplayName("seller không sở hữu sản phẩm bị từ chối")
  void createAuction_rejectsWrongOwner() {
    LocalDateTime start = validStart();
    LocalDateTime end = validEnd(start);
    Item otherItem = AuctionTestFixtures.sampleItem(ITEM_ID, "other-seller");
    when(itemService.findById(ITEM_ID)).thenReturn(Optional.of(otherItem));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> creationService.createAuction(seller, ITEM_ID, start, end, 0));

    assertTrue(ex.getMessage().contains("quyền"));
  }

  @Test
  @DisplayName("sản phẩm không còn available bị từ chối")
  void createAuction_rejectsUnavailableItem() {
    LocalDateTime start = validStart();
    LocalDateTime end = validEnd(start);
    item.setAvailable(false);
    when(itemService.findById(ITEM_ID)).thenReturn(Optional.of(item));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> creationService.createAuction(seller, ITEM_ID, start, end, 0));

    assertTrue(ex.getMessage().contains("đã được bán"));
  }

  @Test
  @DisplayName("sản phẩm không tồn tại bị từ chối")
  void createAuction_rejectsMissingItem() {
    LocalDateTime start = validStart();
    LocalDateTime end = validEnd(start);
    when(itemService.findById(ITEM_ID)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class,
        () -> creationService.createAuction(seller, ITEM_ID, start, end, 0));
  }
}
