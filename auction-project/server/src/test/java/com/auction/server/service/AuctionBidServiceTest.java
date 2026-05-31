package com.auction.server.service;

import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.testutil.AuctionTestFixtures;
import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionBidService — đặt giá")
class AuctionBidServiceTest {

  private static final String SESSION_ID = "session-abc";
  private static final String SELLER_ID = "seller-1";

  @Mock
  private AuctionSessionDAO auctionSessionDAO;
  @Mock
  private BidDAO bidDAO;
  @Mock
  private UserDAO userDAO;
  @Mock
  private AuctionQueryService queryService;

  private AuctionBidService bidService;
  private Item item;
  private AuctionSession session;

  @BeforeEach
  void setUp() {
    bidService = new AuctionBidService(auctionSessionDAO, bidDAO, userDAO, queryService);
    item = AuctionTestFixtures.sampleItem("item-1", SELLER_ID);
    session = AuctionTestFixtures.runningSession(SESSION_ID, item, SELLER_ID, 1_000_000, null);
    when(queryService.getSessionOrThrow(SESSION_ID)).thenReturn(session);
  }

  @Test
  @DisplayName("đặt giá hợp lệ — đóng băng số dư và lưu bid")
  void placeBid_success() throws Exception {
    Bidder bidder = AuctionTestFixtures.sampleBidder("bidder-1", 5_000_000, 0);

    Bid result = bidService.placeBid(bidder, SESSION_ID, 1_100_000, Bid.BidType.MANUAL);

    assertEquals(1_100_000, result.getAmount());
    assertEquals(Bid.BidType.MANUAL, result.getBidType());
    assertEquals(3_900_000, bidder.getBalance());
    assertEquals(1_100_000, bidder.getFrozenBalance());

    verify(bidDAO).save(any(Bid.class));
    verify(auctionSessionDAO).updateCurrentBid(SESSION_ID, 1_100_000, "bidder-1", "Bidder Test");
    verify(userDAO, atLeastOnce()).updateBidderDetails(bidder);
  }

  @Test
  @DisplayName("phiên không RUNNING thì từ chối bid")
  void placeBid_rejectsWhenNotRunning() {
    session.setStatus(com.auction.enums.AuctionStatus.PENDING);
    Bidder bidder = AuctionTestFixtures.sampleBidder("bidder-1", 5_000_000, 0);

    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> bidService.placeBid(bidder, SESSION_ID, 1_100_000, Bid.BidType.MANUAL));

    assertTrue(ex.getMessage().contains("không nhận bid"));
    verifyNoInteractions(bidDAO);
  }

  @Test
  @DisplayName("seller không được tự đấu giá sản phẩm của mình")
  void placeBid_rejectsSellerSelfBid() {
    Bidder sellerAsBidder = AuctionTestFixtures.sampleBidder(SELLER_ID, 5_000_000, 0);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> bidService.placeBid(sellerAsBidder, SESSION_ID, 1_100_000, Bid.BidType.MANUAL));

    assertTrue(ex.getMessage().contains("không thể tự đấu giá"));
  }

  @Test
  @DisplayName("giá thấp hơn currentPrice + bidIncrement bị từ chối")
  void placeBid_rejectsAmountTooLow() {
    Bidder bidder = AuctionTestFixtures.sampleBidder("bidder-1", 5_000_000, 0);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> bidService.placeBid(bidder, SESSION_ID, 1_050_000, Bid.BidType.MANUAL));

    assertTrue(ex.getMessage().contains("không hợp lệ"));
    verifyNoInteractions(bidDAO);
  }

  @Test
  @DisplayName("số dư không đủ bị từ chối")
  void placeBid_rejectsInsufficientBalance() {
    Bidder bidder = AuctionTestFixtures.sampleBidder("bidder-1", 500_000, 0);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> bidService.placeBid(bidder, SESSION_ID, 1_100_000, Bid.BidType.MANUAL));

    assertTrue(ex.getMessage().contains("không đủ"));
  }

  @Test
  @DisplayName("tự đè giá — được dùng lại tiền đóng băng cũ")
  void placeBid_selfOutbidReusesFrozenFunds() throws Exception {
    session.setCurrentPrice(2_000_000);
    session.setCurrentWinnerId("bidder-1");
    session.setCurrentWinnerName("Bidder Test");

    Bidder bidder = AuctionTestFixtures.sampleBidder("bidder-1", 500_000, 2_000_000);

    bidService.placeBid(bidder, SESSION_ID, 2_100_000, Bid.BidType.MANUAL);

    assertEquals(400_000, bidder.getBalance());
    assertEquals(2_100_000, bidder.getFrozenBalance());
    verify(bidDAO).save(any(Bid.class));
  }

  @Test
  @DisplayName("vượt giá người khác — hoàn trả số dư cho người thua")
  void placeBid_refundsPreviousWinner() throws Exception {
    Bidder oldWinner = AuctionTestFixtures.sampleBidder("old-winner", 1_000_000, 2_000_000);
    Bidder newBidder = AuctionTestFixtures.sampleBidder("new-bidder", 5_000_000, 0);

    session.setCurrentPrice(2_000_000);
    session.setCurrentWinnerId("old-winner");
    when(userDAO.findById("old-winner")).thenReturn(Optional.of(oldWinner));

    bidService.placeBid(newBidder, SESSION_ID, 2_100_000, Bid.BidType.MANUAL);

    assertEquals(3_000_000, oldWinner.getBalance());
    assertEquals(0, oldWinner.getFrozenBalance());
    verify(userDAO).updateBidderDetails(oldWinner);
  }

  @Test
  @DisplayName("đặt giá AUTO tạo bid loại AUTO")
  void placeBid_autoBidType() throws Exception {
    Bidder bidder = AuctionTestFixtures.sampleBidder("bidder-1", 5_000_000, 0);

    Bid result = bidService.placeBid(bidder, SESSION_ID, 1_100_000, Bid.BidType.AUTO);

    assertEquals(Bid.BidType.AUTO, result.getBidType());
    assertTrue(result.isAutoBid());
  }
}
