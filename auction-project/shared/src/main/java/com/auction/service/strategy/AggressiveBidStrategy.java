package com.auction.service.strategy;

public class AggressiveBidStrategy implements AutoBidStrategy {

  @Override
  public double calculateNextBid(double currentPrice, double maxBudget, double minIncrement) {
    double jump = minIncrement * 1.5;
    double nextBid = currentPrice + jump;

    // Nếu vượt ngạch thì thôi (bot ngừng hoạt động cho phiên này)
    if (nextBid > maxBudget) {
      return -1;
    }

    return nextBid;
  }
}

