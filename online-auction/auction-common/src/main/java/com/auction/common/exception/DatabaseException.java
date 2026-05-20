package com.auction.common.exception;

/**
 * ============================================================
 * DatabaseException — Lỗi tầng dữ liệu (DAO / SQL)
 * ============================================================
 *
 * Dùng khi:
 *   - Không kết nối được database
 *   - SQL query thất bại
 *   - Không tìm thấy entity theo ID
 *   - Vi phạm ràng buộc dữ liệu (UNIQUE, NOT NULL...)
 *
 * Luôn wrap nguyên nhân gốc (SQLException) để dễ trace.
 * ============================================================
 */
public class DatabaseException extends AuctionException {

  public DatabaseException(String message) {
    super("DATABASE_ERROR", message);
  }

  public DatabaseException(String message, Throwable cause) {
    super("DATABASE_ERROR", message, cause);
  }

  public DatabaseException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  // ===== Factory methods =====

  /** Lỗi SQL chung, wrap nguyên nhân gốc */
  public static DatabaseException queryFailed(String operation, Throwable cause) {
    return new DatabaseException("Lỗi database khi " + operation + ": " + cause.getMessage(), cause);
  }

  /** Không tìm thấy entity theo ID */
  public static DatabaseException notFound(String entityType, String id) {
    return new DatabaseException("NOT_FOUND",
        "Không tìm thấy " + entityType + " với ID: " + id, null);
  }

  /** Lỗi kết nối */
  public static DatabaseException connectionFailed(Throwable cause) {
    return new DatabaseException("DB_CONNECTION_ERROR",
        "Không thể kết nối database: " + cause.getMessage(), cause);
  }
}
