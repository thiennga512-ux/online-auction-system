package com.auction.server.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.service.auction.AuctionSession;

/**
 * ============================================================
 * AuctionCreationService — SRP: Tạo phiên đấu giá mới
 * ============================================================
 *
 * Chịu trách nhiệm DUY NHẤT: validate dữ liệu đầu vào và
 * tạo phiên đấu giá mới cho Seller. Không xử lý duyệt/từ chối/bid.
 * ============================================================
 */
public class AuctionCreationService {

  private final AuctionSessionDAO auctionSessionDAO;
  private final ItemService itemService;

  // Lock theo item để tránh tạo trùng nhiều phiên cho cùng 1 sản phẩm
  private final Map<String, Object> itemLocks = new ConcurrentHashMap<>();

  public AuctionCreationService(AuctionSessionDAO auctionSessionDAO, ItemService itemService) {
    this.auctionSessionDAO = auctionSessionDAO;
    this.itemService = itemService;
  }

  /**
   * Seller tạo một phiên đấu giá mới cho sản phẩm của mình.
   *
   * @param seller             Seller tạo phiên
   * @param itemId             ID sản phẩm muốn đấu giá
   * @param startTime          Thời gian bắt đầu nhận bid
   * @param endTime            Thời gian kết thúc dự kiến
   * @param antiSnipingSeconds Giây gia hạn anti-sniping (0 = tắt)
   * @return AuctionSession mới tạo (trạng thái PENDING)
   */
  public AuctionSession createAuction(Seller seller, String itemId,
      LocalDateTime startTime, LocalDateTime endTime, int antiSnipingSeconds) {
    Object lock = itemLocks.computeIfAbsent(itemId, k -> new Object());
    synchronized (lock) {

      // Bước 1: Validate thời gian
      validateAuctionTime(startTime, endTime);

      // Bước 2: Lấy Item và kiểm tra quyền sở hữu
      Item item = itemService.findById(itemId)
          .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + itemId));

      if (!item.getSellerId().equals(seller.getId())) {
        throw new IllegalArgumentException("Bạn không có quyền đấu giá sản phẩm này");
      }
      if (!item.isAvailable()) {
        throw new IllegalArgumentException("Sản phẩm này đã được bán hoặc đang trong phiên đấu giá khác");
      }

      // Bước 3: Tạo AuctionSession mới (trạng thái PENDING)
      AuctionSession session = new AuctionSession(
          item, seller.getId(), seller.getFullName(),
          startTime, endTime, antiSnipingSeconds);

      // Bước 4: Lưu vào database
      try {
        auctionSessionDAO.save(session);
        itemService.markItemAsSold(itemId);
        item.setAvailable(false);
        System.out.printf("[AuctionCreationService] Tạo phiên mới: %s | Sản phẩm: %s%n",
            session.getId().substring(0, 8), item.getName());
      } catch (SQLException e) {
        throw new RuntimeException("Lỗi khi tạo phiên đấu giá: " + e.getMessage(), e);
      }

      return session;
    }
  }

  /** Validate thời gian phiên đấu giá */
  private void validateAuctionTime(LocalDateTime startTime, LocalDateTime endTime) {
    LocalDateTime now = LocalDateTime.now();

    if (startTime.isBefore(now.plusMinutes(5))) {
      throw new IllegalArgumentException("Thời gian bắt đầu phải ít nhất 5 phút từ bây giờ");
    }
    if (!endTime.isAfter(startTime.plusHours(1))) {
      throw new IllegalArgumentException("Phiên đấu giá phải kéo dài ít nhất 1 giờ");
    }
    if (endTime.isAfter(startTime.plusDays(30))) {
      throw new IllegalArgumentException("Phiên đấu giá không được kéo dài quá 30 ngày");
    }
  }
}