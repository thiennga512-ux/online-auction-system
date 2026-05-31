package com.auction.server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

  @Test
  void hash_sameInput_producesSameOutput() {
    String h1 = PasswordHasher.hash("bidder@123");
    String h2 = PasswordHasher.hash("bidder@123");
    assertEquals(h1, h2);
    assertEquals(64, h1.length());
  }

  @Test
  void hash_differentInput_producesDifferentOutput() {
    assertNotEquals(
        PasswordHasher.hash("password1"),
        PasswordHasher.hash("password2"));
  }
}
