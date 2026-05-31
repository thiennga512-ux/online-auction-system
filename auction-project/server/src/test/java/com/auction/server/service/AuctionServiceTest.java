package com.auction.server.service;

import com.auction.enums.AuctionStatus;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.testutil.TestFixtures;
import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

  private static final String SELLER_ID = "seller-1";
  private static final String ITEM_ID = "item-1";
  private static final String SESSION_ID = "session-1";

  @Mock
  private AuctionSessionDAO auctionSessionDAO;
  @Mock
  private BidDAO bidDAO;
  @Mock
  private UserDAO userDAO;
  @Mock
  private ItemService itemService;

  private AuctionService auctionService;
  private Seller seller;
  private Item item;

  @BeforeEach
  void setUp() {
    auctionService = new AuctionService(auctionSessionDAO, bidDAO, userDAO, itemService);
    seller = TestFixtures.seller(SELLER_ID);
    item = TestFixtures.availableItem(ITEM_ID, SELLER_ID);
  }

  // --- createAuction / validateAuctionTime ---

  @Test
  void createAuction_validTimes_savesSession() throws Exception {
    when(itemService.findById(ITEM_ID)).thenReturn(Optional.of(item));
    LocalDateTime start = TestFixtures.NOW.plusMinutes(10);
    LocalDateTime end = start.plusHours(2);

    AuctionSession session = auctionService.createAuction(
        seller, ITEM_ID, start, end, 30);

    assertEquals(AuctionStatus.PENDING, session.getStatus());
    verify(auctionSessionDAO).save(session);
    verify(itemService).markItemAsSold(ITEM_ID);
  }

  @Test
  void createAuction_endBeforeStart_throws() {
    LocalDateTime start = TestFixtures.NOW.plusHours(2);
    LocalDateTime end = start.minusMinutes(1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> auctionService.createAuction(seller, ITEM_ID, start, end, 0));
    assertTrue(ex.getMessage().contains("kết thúc phải sau"));
  }

  @Test
  void createAuction_durationOver30Days_throws() {
    LocalDateTime start = TestFixtures.NOW;
    LocalDateTime end = start.plusDays(31);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> auctionService.createAuction(seller, ITEM_ID, start, end, 0));
    assertTrue(ex.getMessage().contains("30 ngày"));
  }

  @Test
  void createAuction_notOwner_throws() throws Exception {
    Item otherItem = TestFixtures.availableItem(ITEM_ID, "other-seller");
    when(itemService.findById(ITEM_ID)).thenReturn(Optional.of(otherItem));

    assertThrows(IllegalArgumentException.class,
        () -> auctionService.createAuction(seller, ITEM_ID,
            TestFixtures.NOW, TestFixtures.NOW.plusHours(1), 0));
  }

  @Test
  void createAuction_itemUnavailable_throws() throws Exception {
    item.setAvailable(false);
    when(itemService.findById(ITEM_ID)).thenReturn(Optional.of(item));

    assertThrows(IllegalArgumentException.class,
        () -> auctionService.createAuction(seller, ITEM_ID,
            TestFixtures.NOW, TestFixtures.NOW.plusHours(1), 0));
  }

  // --- approve / reject / cancel ---

  @Test
  void approveAuction_pendingSession_ok() throws Exception {
    AuctionSession session = TestFixtures.pendingSession(SESSION_ID, item, SELLER_ID);
    when(auctionSessionDAO.findById(SESSION_ID)).thenReturn(Optional.of(session));

    auctionService.approveAuction("admin-1", SESSION_ID);

    verify(auctionSessionDAO).approve(SESSION_ID, "admin-1");
  }

  @Test
  void approveAuction_notPending_throws() throws Exception {
    AuctionSession session = TestFixtures.runningSession(SESSION_ID, item, SELLER_ID);
    when(auctionSessionDAO.findById(SESSION_ID)).thenReturn(Optional.of(session));

    assertThrows(IllegalStateException.class,
        () -> auctionService.approveAuction("admin-1", SESSION_ID));
  }

  @Test
  void cancelAuction_bidderCannotCancel() throws Exception {
    AuctionSession session = TestFixtures.runningSession(SESSION_ID, item, SELLER_ID);
    when(auctionSessionDAO.findById(SESSION_ID)).thenReturn(Optional.of(session));
    Bidder bidder = TestFixtures.bidder("bidder-1", 0);

    assertThrows(IllegalStateException.class,
        () -> auctionService.cancelAuction(bidder, SESSION_ID));
  }

  @Test
  void cancelAuction_adminCanCancel() throws Exception {
    AuctionSession session = TestFixtures.runningSession(SESSION_ID, item, SELLER_ID);
    when(auctionSessionDAO.findById(SESSION_ID)).thenReturn(Optional.of(session));
    Admin admin = TestFixtures.admin("admin-1");

    auctionService.cancelAuction(admin, SESSION_ID);

    verify(auctionSessionDAO).updateStatus(SESSION_ID, AuctionStatus.CANCELLED);
  }

  // --- placeBid ---

  @Test
  void placeBid_sessionNotRunning_throws() throws Exception {
    AuctionSession session = TestFixtures.pendingSession(SESSION_ID, item, SELLER_ID);
    when(auctionSessionDAO.findById(SESSION_ID)).thenReturn(Optional.of(session));
    Bidder bidder = TestFixtures.bidder("bidder-1", 5_000_000);

    assertThrows(IllegalStateException.class,
        () -> auctionService.placeBid(bidder, SESSION_ID, 2_000_000, Bid.BidType.MANUAL));
  }

  @Test
  void placeBid_amountTooLow_throws() throws Exception {
    AuctionSession session = TestFixtures.runningSession(SESSION_ID, item, SELLER_ID);
    when(auctionSessionDAO.findById(SESSION_ID)).thenReturn(Optional.of(session));
    Bidder bidder = TestFixtures.bidder("bidder-1", 5_000_000);

    // Giá hiện tại 1_000_000 + bước giá 50_000 → tối thiểu 1_050_000
    assertThrows(IllegalArgumentException.class,
        () -> auctionService.placeBid(bidder, SESSION_ID, 1_000_000, Bid.BidType.MANUAL));
  }

  @Test
  void placeBid_insufficientBalance_throws() throws Exception {
    AuctionSession session = TestFixtures.runningSession(SESSION_ID, item, SELLER_ID);
    when(auctionSessionDAO.findById(SESSION_ID)).thenReturn(Optional.of(session));
    Bidder bidder = TestFixtures.bidder("bidder-1", 100_000);

    assertThrows(IllegalArgumentException.class,
        () -> auctionService.placeBid(bidder, SESSION_ID, 1_050_000, Bid.BidType.MANUAL));
  }

  @Test
  void placeBid_validBid_freezesBalanceAndSaves() throws Exception {
    AuctionSession session = TestFixtures.runningSession(SESSION_ID, item, SELLER_ID);
    when(auctionSessionDAO.findById(SESSION_ID)).thenReturn(Optional.of(session));
    Bidder bidder = TestFixtures.bidder("bidder-1", 5_000_000);

    Bid result = auctionService.placeBid(bidder, SESSION_ID, 1_050_000, Bid.BidType.MANUAL);

    assertEquals(1_050_000, result.getAmount());
    assertEquals(3_950_000, bidder.getBalance());
    assertEquals(1_050_000, bidder.getFrozenBalance());
    verify(bidDAO).save(any(Bid.class));
    verify(auctionSessionDAO).updateCurrentBid(
        SESSION_ID, 1_050_000, "bidder-1", bidder.getFullName());
    verify(userDAO).updateBidderDetails(bidder);
  }
}
