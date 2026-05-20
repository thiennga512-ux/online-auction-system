package com.auction.common.model.auction;

import java.io.Serializable;

/**
 * ============================================================
 * Class AutoBidConfig — Cấu hình Auto-bidding của một User
 * ============================================================
 * Lưu trữ thiết lập tự động đấu giá của một Bidder cho một phiên cụ thể.
 * Được truyền qua mạng nên cần implements Serializable.
 * ============================================================
 */
public class AutoBidConfig implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String sessionId;
  private final String bidderId;
  private final double maxBudget;
  private final String strategyType; // "AGGRESSIVE", "CONSERVATIVE"

  public AutoBidConfig(String sessionId, String bidderId, double maxBudget, String strategyType) {
    this.sessionId = sessionId;
    this.bidderId = bidderId;
    this.maxBudget = maxBudget;
    this.strategyType = strategyType;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getBidderId() {
    return bidderId;
  }

  public double getMaxBudget() {
    return maxBudget;
  }

  public String getStrategyType() {
    return strategyType;
  }
}
