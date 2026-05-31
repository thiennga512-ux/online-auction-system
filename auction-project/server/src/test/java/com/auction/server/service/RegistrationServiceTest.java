package com.auction.server.service;

import com.auction.exception.BusinessException;
import com.auction.exception.ValidationException;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.server.dao.UserDAO;
import com.auction.server.testutil.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

  @Mock
  private UserDAO userDAO;

  private RegistrationService registrationService;

  @BeforeEach
  void setUp() {
    registrationService = new RegistrationService(userDAO);
  }

  @Test
  void registerBidder_validInput_savesUser() throws Exception {
    when(userDAO.emailExists(any())).thenReturn(false);
    when(userDAO.usernameExists(any())).thenReturn(false);

    Bidder bidder = registrationService.registerBidder(
        "Đỗ Văn Khiêm", "bidder3@auction.vn", "bidder@789");

    assertEquals("Đỗ Văn Khiêm", bidder.getFullName());
    assertEquals("bidder3@auction.vn", bidder.getEmail());
    verify(userDAO).save(bidder);
  }

  @Test
  void registerBidder_duplicateEmail_throwsBusinessException() throws Exception {
    when(userDAO.emailExists("bidder1@auction.vn")).thenReturn(true);

    assertThrows(BusinessException.class,
        () -> registrationService.registerBidder("A", "bidder1@auction.vn", "password1"));
  }

  @Test
  void registerBidder_shortPassword_throwsValidationException() {
    assertThrows(ValidationException.class,
        () -> registrationService.registerBidder("A", "new@auction.vn", "12345"));
  }

  @Test
  void upgradeToSeller_validBidder_returnsSeller() throws Exception {
    Bidder bidder = TestFixtures.bidder("bidder-1", 0);
    Seller seller = new Seller("bidder-1", TestFixtures.NOW, TestFixtures.NOW,
        "bidder1", TestFixtures.PASSWORD_HASH, "bidder@auction.vn", "Trần Thị Mua", true,
        0, 0, 0, "My Shop", "987654321");

    when(userDAO.citizenIdExists("987654321")).thenReturn(false);
    when(userDAO.findById("bidder-1"))
        .thenReturn(Optional.of(bidder))
        .thenReturn(Optional.of(seller));

    Seller result = registrationService.upgradeToSeller("bidder-1", "My Shop", "987654321");

    assertEquals("My Shop", result.getShopName());
    verify(userDAO).upgradeToSeller("bidder-1", "My Shop", "987654321");
  }

  @Test
  void upgradeToSeller_invalidCitizenId_throwsValidationException() {
    assertThrows(ValidationException.class,
        () -> registrationService.upgradeToSeller("bidder-1", "Shop", "abc"));
  }

  @Test
  void upgradeToSeller_shortShopName_throwsValidationException() {
    assertThrows(ValidationException.class,
        () -> registrationService.upgradeToSeller("bidder-1", "AB", "123456789"));
  }
}
