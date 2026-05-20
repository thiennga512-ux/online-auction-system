package com.auction.common.strategy;

/**
 * ============================================================
 * Interface AutoBidStrategy — Design Pattern: STRATEGY
 * ============================================================
 * Định nghĩa hành vi chung cho bộ máy tự động đấu giá.
 * Các chiến lược (Strategy) khác nhau sẽ có logic nhảy giá khác nhau.
 * Ngăn chặn việc phải viết quá nhiều câu lệnh "if/else" trong AuctionService.
 * ============================================================
 */
public interface AutoBidStrategy {

  /**
   * Tính toán mức giá tiếp theo sẽ đặt.
   *
   * @param currentPrice   Giá hiện tại của phiên đấu giá
   * @param maxBudget      Ngân sách tối đa của User (không được vượt qua)
   * @param minIncrement   Bước giá tối thiểu quy định bởi Item
   * @return Mức giá sẽ đặt, hoặc -1 nếu vượt quá ngân sách (ngừng auto-bid)
   */
  double calculateNextBid(double currentPrice, double maxBudget, double minIncrement);
}
