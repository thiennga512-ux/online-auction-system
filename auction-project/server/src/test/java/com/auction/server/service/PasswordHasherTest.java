package com.auction.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordHasher — mã hoá mật khẩu SHA-256")
class PasswordHasherTest {

  @Test
  @DisplayName("hash trả về chuỗi hex 64 ký tự cố định cho cùng input")
  void hash_isDeterministicAndHexFormat() {
    String hash1 = PasswordHasher.hash("secret123");
    String hash2 = PasswordHasher.hash("secret123");

    assertEquals(hash1, hash2);
    assertEquals(64, hash1.length());
    assertTrue(hash1.matches("[0-9a-f]+"));
  }

  @Test
  @DisplayName("mật khẩu khác nhau cho hash khác nhau")
  void hash_differentPasswordsProduceDifferentHashes() {
    String hashA = PasswordHasher.hash("passwordA");
    String hashB = PasswordHasher.hash("passwordB");

    assertNotEquals(hashA, hashB);
  }

  @Test
  @DisplayName("hash chuỗi rỗng vẫn hợp lệ")
  void hash_emptyString() {
    String hash = PasswordHasher.hash("");
    assertNotNull(hash);
    assertEquals(64, hash.length());
  }
}
