package com.auction.server.service;

import com.auction.exception.BusinessException;
import com.auction.exception.DatabaseException;
import com.auction.model.Bidder;
import com.auction.server.dao.UserDAO;
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
class UserServiceTest {

  @Mock
  private UserDAO userDAO;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userDAO);
  }

  @Test
  void depositForBidder_increasesBalance() throws Exception {
    Bidder bidder = TestFixtures.bidder("bidder-1", 1_000_000);
    when(userDAO.findById("bidder-1")).thenReturn(Optional.of(bidder));

    userService.depositForBidder("bidder-1", 500_000);

    assertEquals(1_500_000, bidder.getBalance());
    verify(userDAO).updateBidderDetails(bidder);
  }

  @Test
  void depositForBidder_unknownId_throwsDatabaseException() throws Exception {
    when(userDAO.findById("missing")).thenReturn(Optional.empty());

    assertThrows(DatabaseException.class,
        () -> userService.depositForBidder("missing", 100_000));
  }

  @Test
  void setUserActive_adminLocksOtherUser_ok() throws Exception {
    userService.setUserActive("admin-1", "bidder-1", false);
    verify(userDAO).setActive("bidder-1", false);
  }

  @Test
  void setUserActive_adminLocksSelf_throwsBusinessException() {
    assertThrows(BusinessException.class,
        () -> userService.setUserActive("admin-1", "admin-1", false));
  }
}
