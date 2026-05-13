package com.auction.exception;

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
private final String errorCode;//Chức năng: Lưu trữ một mã lỗi định danh (ví dụ: USER_NOT_FOUND, INVALID_BID).
//Dùng khi chỉ muốn báo một lỗi chung chung về hệ thống đấu giá mà không cần định nghĩa mã lỗi cụ thể.
public AuctionException(String message){
    super(message);
    this.errorCode = "AUCTION_ERROR";
}
//Dùng khi muốn chỉ rõ lỗi gì đang xảy ra
public AuctionException(String errorCode,String message){
    super(message);
    this.errorCode=errorCode;
}
//Truy vet loi
public AuctionException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
