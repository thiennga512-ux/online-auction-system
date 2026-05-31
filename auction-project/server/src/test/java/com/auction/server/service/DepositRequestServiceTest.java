package com.auction.server.service;

import com.auction.model.Bidder;
import com.auction.server.testutil.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositRequestServiceTest {

  @Mock
  private UserService userService;

  private DepositRequestService depositRequestService;

  @BeforeEach
  void setUp() {
    depositRequestService = new DepositRequestService(userService);
  }

  @Test
  void requestDeposit_validAmount_createsPendingRequest() {
    Bidder bidder = TestFixtures.bidder("bidder-1", 0);
    when(userService.findById("bidder-1")).thenReturn(Optional.of(bidder));

    String requestId = depositRequestService.requestDeposit("bidder-1", 2_000_000);

    assertNotNull(requestId);
    assertEquals(1, depositRequestService.getPendingDeposits().size());
    assertEquals("bidder-1", depositRequestService.getPendingRequest(requestId).bidderId());
  }

  @Test
  void requestDeposit_zeroAmount_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> depositRequestService.requestDeposit("bidder-1", 0));
  }

  @Test
  void approveDeposit_depositsAndRemovesPending() {
    Bidder bidder = TestFixtures.bidder("bidder-1", 1_000_000);
    when(userService.findById("bidder-1")).thenReturn(Optional.of(bidder));

    String requestId = depositRequestService.requestDeposit("bidder-1", 500_000);
    doAnswer(inv -> {
      bidder.setBalance(bidder.getBalance() + 500_000);
      return null;
    }).when(userService).depositForBidder("bidder-1", 500_000);

    double newBalance = depositRequestService.approveDeposit(requestId);

    assertEquals(1_500_000, newBalance);
    assertTrue(depositRequestService.getPendingDeposits().isEmpty());
    verify(userService).depositForBidder("bidder-1", 500_000);
  }

  @Test
  void rejectDeposit_removesWithoutDepositing() {
    Bidder bidder = TestFixtures.bidder("bidder-1", 1_000_000);
    when(userService.findById("bidder-1")).thenReturn(Optional.of(bidder));

    String requestId = depositRequestService.requestDeposit("bidder-1", 500_000);
    var rejected = depositRequestService.rejectDeposit(requestId, "Sai thông tin");

    assertEquals(500_000, rejected.amount());
    assertTrue(depositRequestService.getPendingDeposits().isEmpty());
    verify(userService, never()).depositForBidder(any(), anyDouble());
  }
}
