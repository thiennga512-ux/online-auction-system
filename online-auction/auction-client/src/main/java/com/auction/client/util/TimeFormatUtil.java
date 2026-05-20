package com.auction.client.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeFormatUtil {
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  private TimeFormatUtil() {}

  public static String formatDateTime(LocalDateTime dateTime) {
    if (dateTime == null) {
      return "Không xác định";
    }
    return dateTime.format(DATE_TIME_FORMATTER);
  }

  /**
   * Định dạng thời gian còn lại đến một mốc cụ thể.
   * Dùng chung cho cả "đến khi mở" và "đến khi kết thúc".
   */
  public static String formatRemainingTime(LocalDateTime targetTime) {
    if (targetTime == null) {
      return "Không xác định";
    }

    Duration duration = Duration.between(LocalDateTime.now(), targetTime);
    if (duration.isNegative() || duration.isZero()) {
      return "0 giây";
    }

    long totalSeconds = duration.getSeconds();
    long days    = totalSeconds / 86400;
    long hours   = (totalSeconds % 86400) / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;

    if (days > 0) {
      return String.format("%d ngày %d giờ %d phút", days, hours, minutes);
    }
    if (hours > 0) {
      return String.format("%d giờ %d phút %d giây", hours, minutes, seconds);
    }
    if (minutes > 0) {
      return String.format("%d phút %d giây", minutes, seconds);
    }
    return Math.max(1, seconds) + " giây";
  }

  /**
   * Trả về màu cho label đếm ngược đến kết thúc phiên RUNNING.
   * Xanh lá → Vàng → Đỏ khi sắp hết giờ.
   */
  public static String getEndCountdownColor(LocalDateTime endTime) {
    if (endTime == null) return "#9ca3af";
    Duration d = Duration.between(LocalDateTime.now(), endTime);
    long s = d.getSeconds();
    if (s <= 0)   return "#ef4444"; // Hết giờ
    if (s <= 60)  return "#ef4444"; // < 1 phút: đỏ
    if (s <= 300) return "#f59e0b"; // < 5 phút: vàng
    return "#10b981";               // Bình thường: xanh ngọc
  }

  /**
   * Trả về màu cho label đếm ngược đến khi MỞ phiên OPEN.
   * Luôn màu xanh lam nhạt (trạng thái chờ).
   */
  public static String getOpenCountdownColor(LocalDateTime startTime) {
    if (startTime == null) return "#9ca3af";
    Duration d = Duration.between(LocalDateTime.now(), startTime);
    long s = d.getSeconds();
    if (s <= 0)    return "#9ca3af";  // Đã qua giờ mở (sẽ sớm RUNNING)
    if (s <= 3600) return "#60a5fa";  // < 1 giờ: xanh lam sáng
    return "#93c5fd";                 // Còn lâu: xanh lam nhạt
  }

  /**
   * Trả về màu dựa vào endTime — wrapper ngược chiều cho code cũ.
   */
  public static String getCountdownTextColor(LocalDateTime endTime) {
    return getEndCountdownColor(endTime);
  }
}
