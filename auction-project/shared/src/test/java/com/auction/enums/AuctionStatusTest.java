package com.auction.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionStatus — trạng thái phiên đấu giá")
class AuctionStatusTest {

  @Test
  @DisplayName("chỉ RUNNING mới nhận bid")
  void isAcceptingBids_onlyRunning() {
    assertTrue(AuctionStatus.RUNNING.isAcceptingBids());
    assertFalse(AuctionStatus.PENDING.isAcceptingBids());
    assertFalse(AuctionStatus.OPEN.isAcceptingBids());
    assertFalse(AuctionStatus.FINISHED.isAcceptingBids());
    assertFalse(AuctionStatus.CANCELLED.isAcceptingBids());
  }

  @ParameterizedTest
  @EnumSource(value = AuctionStatus.class, names = {"FINISHED", "CANCELLED"})
  @DisplayName("FINISHED và CANCELLED là trạng thái kết thúc")
  void isTerminal_finishedOrCancelled(AuctionStatus status) {
    assertTrue(status.isTerminal());
  }

  @ParameterizedTest
  @EnumSource(value = AuctionStatus.class, names = {"PENDING", "OPEN", "RUNNING"})
  @DisplayName("PENDING, OPEN, RUNNING chưa kết thúc")
  void isTerminal_notTerminalForActiveStates(AuctionStatus status) {
    assertFalse(status.isTerminal());
  }

  @Test
  @DisplayName("fromString chuyển đổi không phân biệt hoa thường")
  void fromString_parsesCaseInsensitive() {
    assertEquals(AuctionStatus.RUNNING, AuctionStatus.fromString("running"));
    assertEquals(AuctionStatus.PENDING, AuctionStatus.fromString("PENDING"));
  }

  @Test
  @DisplayName("fromString null hoặc không hợp lệ ném lỗi")
  void fromString_invalidValues() {
    assertThrows(IllegalArgumentException.class, () -> AuctionStatus.fromString(null));
    assertThrows(IllegalArgumentException.class, () -> AuctionStatus.fromString("UNKNOWN"));
  }
}
