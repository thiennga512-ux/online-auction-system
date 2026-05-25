package com.auction.server.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * ============================================================
 * Class PasswordHasher — Tiện ích hash mật khẩu
 * ============================================================
 *
 * 🎯 SOLID — Single Responsibility Principle (SRP):
 * Class này CHỈ làm một việc duy nhất: hash mật khẩu.
 * Được tách ra khỏi AuthService và RegistrationService
 * để cả hai có thể dùng chung mà không lặp code (DRY).
 *
 * 🎓 SHA-256 là gì?
 *   Thuật toán hash một chiều của NSA (National Security Agency).
 *   Input : chuỗi bất kỳ
 *   Output: luôn 64 ký tự hex (256 bits)
 *   Đặc điểm: không thể đảo ngược, thay đổi 1 ký tự → output khác hoàn toàn
 *
 *   "abc" → SHA-256 → "ba7816bf8f01cfea414140de5dae2223..."
 *
 *   ⚠️ Production nên dùng BCrypt (có salt) để chống rainbow table attack.
 *   Đây là Giai đoạn 1 → SHA-256 đủ để học tập.
 * ============================================================
 */
public final class PasswordHasher {

  /** Utility class — không cho phép khởi tạo */
  private PasswordHasher() {}

  /**
   * Hash mật khẩu bằng SHA-256.
   *
   * @param password mật khẩu gốc (plain text)
   * @return chuỗi hex 64 ký tự
   * @throws RuntimeException nếu JVM không hỗ trợ SHA-256 (không bao giờ xảy ra)
   */
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



