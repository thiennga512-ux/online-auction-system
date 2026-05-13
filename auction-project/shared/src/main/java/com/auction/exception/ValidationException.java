package com.auction.exception;
/**
 * ============================================================
 * ValidationException — Lỗi xác thực dữ liệu đầu vào
 * ============================================================
 *
 * Dùng khi:
 *   - Trường bắt buộc bị để trống
 *   - Định dạng không hợp lệ (email, ngày sinh, CCCD...)
 *   - Giá trị ngoài phạm vi cho phép
 *   - Dữ liệu bị thiếu trong request payload
 * ============================================================
 */
public class ValidationException extends AuctionException {

  private final String fieldName;

  public ValidationException(String message) {
    super("VALIDATION_ERROR", message);
    this.fieldName = null;
  }

  public ValidationException(String fieldName, String message) {
    super("VALIDATION_ERROR", message);
    this.fieldName = fieldName;
  }

  public String getFieldName() {
    return fieldName;
  }

  // ===== Factory methods =====

  /** Trường bắt buộc bị để trống */
  public static ValidationException required(String field) {
    return new ValidationException(field, field + " không được để trống.");
  }

  /** Payload request bị null hoặc thiếu */
  public static ValidationException missingPayload() {
    return new ValidationException("Dữ liệu yêu cầu bị thiếu hoặc không hợp lệ.");
  }

  /** Giá trị số phải lớn hơn 0 */
  public static ValidationException mustBePositive(String field) {
    return new ValidationException(field, field + " phải lớn hơn 0.");
  }

  /** Định dạng email không hợp lệ */
  public static ValidationException invalidEmail(String email) {
    return new ValidationException("email", "Email không hợp lệ: " + email);
  }

  /** Mật khẩu quá ngắn */
  public static ValidationException passwordTooShort(int minLength) {
    return new ValidationException("password", "Mật khẩu phải ít nhất " + minLength + " ký tự.");
  }
  /** Giá trị String quá ngắn */
  public static ValidationException tooShort(String field, int minLength) {
    return new ValidationException(field, field + " phải có ít nhất " + minLength + " ký tự.");
  }
}
