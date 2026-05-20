package com.auction.server.observer;

import com.auction.common.network.Response;

/**
 * Interface cho Observer nhận cập nhật từ AuctionBroadcaster.
 * ClientHandler sẽ implement interface này.
 */
public interface ClientObserver {
  /**
   * Phương thức được gọi khi có thông báo mới (ví dụ có người Bid).
   * @param response gói dữ liệu chứa thông báo để gửi về client.
   */
  void onUpdate(Response response);
  
  /**
   * Lấy ID của user đang kết nối (để không tự gửi lại bid của chính mình nếu cần).
   */
  String getUserId();
}
