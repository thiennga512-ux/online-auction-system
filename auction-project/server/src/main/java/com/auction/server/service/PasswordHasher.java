package com.auction.server.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PasswordHasher {

  /** Utility class — không cho phép khởi tạo */
  private PasswordHasher() {
  }

  public static String hash(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(password.getBytes());
      return HexFormat.of().formatHex(hashBytes); // Java 17+
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 luôn có sẵn trong Java → không bao giờ xảy ra
      throw new RuntimeException("SHA-256 không khả dụng", e);
    }
  }
}
