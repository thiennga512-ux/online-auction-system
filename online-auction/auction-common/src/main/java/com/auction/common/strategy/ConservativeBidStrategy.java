package com.auction.common.strategy;

/**
 * ============================================================
 * Class ConservativeBidStrategy — Chiến Thuật An Toàn
 * ============================================================
 * Đặt giá đúng chuẩn minIncrement để tối ưu hóa ngân sách.
 * Tiết kiệm, chậm mà chắc.
 * ============================================================
 */
public class ConservativeBidStrategy implements AutoBidStrategy {

  @Override
  public double calculateNextBid(double currentPrice, double maxBudget, double minIncrement) {
    double nextBid = currentPrice + minIncrement;

    if (nextBid > maxBudget) {
      return -1;
    }

    return nextBid;
  }
}
