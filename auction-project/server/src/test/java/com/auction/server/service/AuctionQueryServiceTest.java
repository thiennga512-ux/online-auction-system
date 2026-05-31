package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.testutil.AuctionTestFixtures;
import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionQueryService — truy vấn phiên đấu giá")
class AuctionQueryServiceTest {

  @Mock
  private AuctionSessionDAO auctionSessionDAO;
  @Mock
  private BidDAO bidDAO;

  private AuctionQueryService queryService;

  @BeforeEach
  void setUp() {
    queryService = new AuctionQueryService(auctionSessionDAO, bidDAO);
  }

  @Test
  @DisplayName("getSessionOrThrow trả về phiên khi tồn tại")
  void getSessionOrThrow_found() throws Exception {
    var item = AuctionTestFixtures.sampleItem("item-1", "seller-1");
    var session = AuctionTestFixtures.runningSession("session-1", item, "seller-1", 1_000_000, null);
    when(auctionSessionDAO.findById("session-1")).thenReturn(Optional.of(session));

    AuctionSession result = queryService.getSessionOrThrow("session-1");

    assertEquals("session-1", result.getId());
  }

  @Test
  @DisplayName("getSessionOrThrow ném lỗi khi không tìm thấy")
  void getSessionOrThrow_notFound() throws Exception {
    when(auctionSessionDAO.findById("missing")).thenReturn(Optional.empty());

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> queryService.getSessionOrThrow("missing"));

    assertTrue(ex.getMessage().contains("Không tìm thấy"));
  }

  @Test
  @DisplayName("getBidsBySession trả về lịch sử bid")
  void getBidsBySession_returnsBids() throws Exception {
    var item = AuctionTestFixtures.sampleItem("item-1", "seller-1");
    var session = AuctionTestFixtures.runningSession("session-1", item, "seller-1", 1_000_000, null);
    when(auctionSessionDAO.findById("session-1")).thenReturn(Optional.of(session));

    Bid bid = Bid.createManual("session-1", "b1", "A", 1_100_000);
    when(bidDAO.findBySessionId("session-1")).thenReturn(List.of(bid));

    List<Bid> bids = queryService.getBidsBySession("session-1");

    assertEquals(1, bids.size());
    assertEquals(1_100_000, bids.get(0).getAmount());
  }
}
