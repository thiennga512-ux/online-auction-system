package com.auction.service.strategy;

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
