package com.auction.server.service;

import com.auction.exception.AuthException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserDAO userDAO;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userDAO);
  }

  @Test
  void login_validCredentials_returnsUser() {
    Bidder bidder = TestFixtures.bidder("bidder-1", 1_000_000);
    when(userDAO.findByEmail("bidder@auction.vn")).thenReturn(Optional.of(bidder));

    var user = authService.login("bidder@auction.vn", TestFixtures.PASSWORD);

    assertEquals("bidder-1", user.getId());
    assertEquals("bidder@auction.vn", user.getEmail());
  }

  @Test
  void login_unknownEmail_throwsAuthException() {
    when(userDAO.findByEmail("unknown@auction.vn")).thenReturn(Optional.empty());

    AuthException ex = assertThrows(AuthException.class,
        () -> authService.login("unknown@auction.vn", "any"));
    assertTrue(ex.getMessage().contains("Email hoặc mật khẩu"));
  }

  @Test
  void login_wrongPassword_throwsAuthException() {
    Bidder bidder = TestFixtures.bidder("bidder-1", 0);
    when(userDAO.findByEmail("bidder@auction.vn")).thenReturn(Optional.of(bidder));

    assertThrows(AuthException.class,
        () -> authService.login("bidder@auction.vn", "wrong-password"));
  }

  @Test
  void login_lockedAccount_throwsAuthException() {
    Bidder bidder = TestFixtures.bidder("bidder-1", 0);
    bidder.setActive(false);
    when(userDAO.findByEmail("bidder@auction.vn")).thenReturn(Optional.of(bidder));

    AuthException ex = assertThrows(AuthException.class,
        () -> authService.login("bidder@auction.vn", TestFixtures.PASSWORD));
    assertTrue(ex.getMessage().contains("bị khoá"));
  }
}
