package com.auction.common.network;

/**
 * ============================================================
 * Enum ActionType — Các loại hành động (API endpoints)
 * ============================================================
 *
 * Định nghĩa tất cả các thao tác mà Client có thể gửi cho Server,
 * cũng như các sự kiện Server gửi (broadcast) về cho Client.
 *
 * Nó tương đương với URL path trong REST API (VD: /api/v1/login).
 * ============================================================
 */
public enum ActionType {
  // --- Thao tác User ---
  LOGIN,
  LOGOUT,
  REGISTER_USER,
  UPGRADE_TO_SELLER,

  // --- Thao tác Item & Auction ---
  LIST_ITEM,
  CREATE_AUCTION,
  GET_ACTIVE_AUCTIONS,  // Lấy danh sách phiên đang chạy
  GET_PENDING_AUCTIONS, // Lấy danh sách phiên chờ Admin duyệt
  GET_AUCTION_BIDS,     // Lấy lịch sử bid của 1 phiên
  GET_MY_ITEMS,         // Lấy danh sách sản phẩm của Seller
  GET_MY_AUCTIONS,      // Lấy danh sách phiên đấu giá của Seller
  APPROVE_AUCTION,      // Admin duyệt phiên
  REJECT_AUCTION,       // Admin từ chối phiên
  CANCEL_AUCTION,       // Admin hoặc Seller huỷ phiên đang chạy/mở

  // --- Realtime Bidding ---
  SUBSCRIBE_AUCTION,    // Client mở hộp thoại đấu giá → Đăng ký nhận realtime update
  UNSUBSCRIBE_AUCTION,  // Client đóng hộp thoại
  PLACE_BID,            // Đặt giá
  REGISTER_NOTIFICATION,// Đăng ký nhận thông báo vào phiên đấu giá

  // --- Broadcast (Server -> Client) ---
  NEW_BID_BROADCAST,        // Server thông báo có bid mới
  AUCTION_STARTED_BROADCAST,    // Server thông báo phiên bắt đầu
  AUCTION_ENDED_BROADCAST,  // Server thông báo phiên kết thúc
  NEW_AUCTION_BROADCAST,    // Server thông báo có phiên mới được duyệt
  GLOBAL_NOTIFICATION_BROADCAST, // Thông báo chuông toàn cục


  // --- Admin quản lý User ---
  GET_ALL_USERS,        // Admin lấy danh sách tất cả user
  SET_USER_ACTIVE,      // Admin khoá / mở khoá tài khoản
  DEPOSIT_BALANCE,      // Admin nạp tiền cho Bidder (demo / testing)

  // --- Bidder tự nạp tiền ---
  SELF_DEPOSIT,         // Bidder tự nạp tiền vào tài khoản của mình
  SYNC_BALANCE,         // Server chủ động đồng bộ số dư mới về Client

  // --- Auto-Bidding ---
  REGISTER_AUTO_BID,    // Đăng ký đấu giá tự động

  // --- Ping / Pong ---
  PING
}
