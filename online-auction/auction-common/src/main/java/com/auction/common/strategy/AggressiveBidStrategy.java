package com.auction.common.strategy;

/**
 * ============================================================
 * Class AggressiveBidStrategy — Chiến Thuật Quyết Liệt
 * ============================================================
 * Đặt giá cao gấp rưỡi (1.5x) bước giá tối thiểu nhằm gây 
 * áp lực tâm lý cho đối thủ.
 * ============================================================
 */
public class AggressiveBidStrategy implements AutoBidStrategy {

  @Override
  public double calculateNextBid(double currentPrice, double maxBudget, double minIncrement) {
    // Nhảy một phát gấp 1.5 lần minIncrement (để hù dọa)
    double jump = minIncrement * 1.5;
    double nextBid = currentPrice + jump;

    // Nếu vượt ngạch thì thôi (bot ngừng hoạt động cho phiên này)
    if (nextBid > maxBudget) {
      return -1;
    }

    return nextBid;
  }
}
