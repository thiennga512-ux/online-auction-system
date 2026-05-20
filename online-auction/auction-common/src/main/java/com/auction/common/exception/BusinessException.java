package com.auction.common.exception;

/**
 * ============================================================
 * BusinessException — Lỗi vi phạm nghiệp vụ
 * ============================================================
 *
 * Dùng khi:
 *   - Đặt giá thấp hơn giá sàn
 *   - Số dư không đủ
 *   - Thao tác sai trạng thái phiên (OPEN/RUNNING/FINISHED)
 *   - Không có quyền sở hữu sản phẩm / phiên
 *   - Email đã đăng ký
 *   - CCCD đã được dùng
 * ============================================================
 */
public class BusinessException extends AuctionException {

  public BusinessException(String message) {
    super("BUSINESS_ERROR", message);
  }

  public BusinessException(String errorCode, String message) {
    super(errorCode, message);
  }

  // ===== Auction Session =====

  public static BusinessException sessionNotInCorrectState(String expected, String actual) {
    return new BusinessException("SESSION_STATE_ERROR",
        "Phiên phải ở trạng thái " + expected + ". Hiện tại: " + actual);
  }

  public static BusinessException sessionAlreadyTerminal() {
    return new BusinessException("SESSION_TERMINAL",
        "Phiên đã kết thúc hoặc đã bị huỷ, không thể thay đổi.");
  }

  public static BusinessException sessionNotAcceptingBids(String status) {
    return new BusinessException("SESSION_NOT_RUNNING",
        "Phiên không nhận bid ở trạng thái: " + status);
  }

  // ===== Bidding =====

  public static BusinessException bidTooLow(double minAmount) {
    return new BusinessException("BID_TOO_LOW",
        String.format("Giá đặt không hợp lệ. Tối thiểu: %,.0f VND", minAmount));
  }

  public static BusinessException insufficientBalance(double required) {
    return new BusinessException("INSUFFICIENT_BALANCE",
        String.format("Số dư ký quỹ không đủ. Cần ít nhất: %,.0f VND", required));
  }

  // ===== Item / Ownership =====

  public static BusinessException itemNotOwnedBySeller() {
    return new BusinessException("ITEM_NOT_OWNED",
        "Bạn không có quyền đấu giá sản phẩm này.");
  }

  public static BusinessException itemNotAvailable() {
    return new BusinessException("ITEM_NOT_AVAILABLE",
        "Sản phẩm này đã được bán hoặc đang trong phiên đấu giá khác.");
  }

  // ===== User / Account =====

  public static BusinessException emailAlreadyRegistered(String email) {
    return new BusinessException("EMAIL_TAKEN",
        "Email đã được đăng ký: " + email);
  }

  public static BusinessException citizenIdAlreadyUsed() {
    return new BusinessException("CITIZEN_ID_TAKEN",
        "Số CCCD này đã được dùng cho tài khoản Seller khác.");
  }

  public static BusinessException alreadySeller() {
    return new BusinessException("ALREADY_SELLER",
        "Tài khoản này đã là Seller!");
  }

  public static BusinessException canOnlyUpgradeFromBidder() {
    return new BusinessException("UPGRADE_NOT_ALLOWED",
        "Chỉ có thể nâng cấp từ Bidder thành Seller.");
  }

  public static BusinessException cannotLockOwnAccount() {
    return new BusinessException("SELF_LOCK_NOT_ALLOWED",
        "Admin không thể tự khoá tài khoản của mình.");
  }

  // ===== Auction Time =====

  public static BusinessException startTimeTooSoon() {
    return new BusinessException("START_TIME_TOO_SOON",
        "Thời gian bắt đầu phải ít nhất 5 phút từ bây giờ.");
  }

  public static BusinessException durationTooShort() {
    return new BusinessException("DURATION_TOO_SHORT",
        "Phiên đấu giá phải kéo dài ít nhất 1 giờ.");
  }

  public static BusinessException durationTooLong() {
    return new BusinessException("DURATION_TOO_LONG",
        "Phiên đấu giá không được kéo dài quá 30 ngày.");
  }
}
