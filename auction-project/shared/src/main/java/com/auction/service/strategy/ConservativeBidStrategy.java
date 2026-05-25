package com.auction.service.strategy;

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
