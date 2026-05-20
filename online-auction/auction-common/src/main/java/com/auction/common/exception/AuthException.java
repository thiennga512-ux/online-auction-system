package com.auction.common.exception;

/**
 * ============================================================
 * AuthException — Lỗi xác thực và phân quyền
 * ============================================================
 *
 * Dùng khi:
 *   - Đăng nhập sai email/mật khẩu
 *   - Tài khoản bị khoá
 *   - Chưa đăng nhập nhưng truy cập tính năng cần login
 *   - Không đủ quyền (Admin-only, Bidder-only...)
 * ============================================================
 */
public class AuthException extends AuctionException {

  public AuthException(String message) {
    super("AUTH_ERROR", message);
  }

  /** Đăng nhập thất bại (email/mật khẩu sai) */
  public static AuthException invalidCredentials() {
    return new AuthException("Email hoặc mật khẩu không đúng.");
  }

  /** Tài khoản bị khoá */
  public static AuthException accountLocked() {
    return new AuthException("Tài khoản đã bị khoá. Liên hệ Admin để được hỗ trợ.");
  }

  /** Chưa đăng nhập */
  public static AuthException notLoggedIn() {
    return new AuthException("Bạn cần đăng nhập để thực hiện thao tác này.");
  }

  /** Không đủ quyền */
  public static AuthException forbidden(String role) {
    return new AuthException("Chỉ " + role + " mới có quyền thực hiện thao tác này.");
  }
}
