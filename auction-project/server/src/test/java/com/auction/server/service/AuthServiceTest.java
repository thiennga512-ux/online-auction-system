package com.auction.server.service;

import com.auction.exception.AuthException;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;
import com.auction.server.testutil.AuctionTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — đăng nhập")
class AuthServiceTest {

  @Mock
  private UserDAO userDAO;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userDAO);
  }

  @Test
  @DisplayName("đăng nhập thành công với email và mật khẩu đúng")
  void login_success() {
    User user = AuctionTestFixtures.bidderWithHashedPassword("myPassword");
    when(userDAO.findByEmail("user@test.com")).thenReturn(Optional.of(user));

    User result = authService.login("user@test.com", "myPassword");

    assertSame(user, result);
    verify(userDAO).findByEmail("user@test.com");
  }

  @Test
  @DisplayName("email không tồn tại ném AuthException")
  void login_unknownEmail() {
    when(userDAO.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

    AuthException ex = assertThrows(AuthException.class,
        () -> authService.login("unknown@test.com", "pass"));

    assertTrue(ex.getMessage().contains("Email hoặc mật khẩu"));
  }

  @Test
  @DisplayName("mật khẩu sai ném AuthException")
  void login_wrongPassword() {
    User user = AuctionTestFixtures.bidderWithHashedPassword("correct");
    when(userDAO.findByEmail("user@test.com")).thenReturn(Optional.of(user));

    assertThrows(AuthException.class,
        () -> authService.login("user@test.com", "wrong"));
  }

  @Test
  @DisplayName("tài khoản bị khoá ném AuthException")
  void login_lockedAccount() {
    User user = AuctionTestFixtures.bidderWithHashedPassword("pass");
    user.setActive(false);
    when(userDAO.findByEmail("user@test.com")).thenReturn(Optional.of(user));

    AuthException ex = assertThrows(AuthException.class,
        () -> authService.login("user@test.com", "pass"));

    assertTrue(ex.getMessage().contains("bị khoá"));
  }
}
