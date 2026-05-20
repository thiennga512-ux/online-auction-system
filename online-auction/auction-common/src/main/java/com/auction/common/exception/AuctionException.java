package com.auction.common.exception;

/**
 * ============================================================
 * Base Exception — Gốc của tất cả exception trong hệ thống
 * ============================================================
 *
 * Phân cấp:
 *   AuctionException
 *     ├── AuthException        : Xác thực, phân quyền
 *     ├── ValidationException  : Dữ liệu đầu vào không hợp lệ
 *     ├── BusinessException    : Vi phạm nghiệp vụ (bid, balance, session...)
 *     └── DatabaseException    : Lỗi tầng dữ liệu
 * ============================================================
 */
public class AuctionException extends RuntimeException {

  private final String errorCode;

  public AuctionException(String message) {
    super(message);
    this.errorCode = "AUCTION_ERROR";
  }

  public AuctionException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public AuctionException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
