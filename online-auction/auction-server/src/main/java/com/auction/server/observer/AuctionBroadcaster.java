package com.auction.server.observer;

import com.auction.common.network.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================
 * Class AuctionBroadcaster — Quản lý sự kiện (Observer Pattern)
 * ============================================================
 *
 * 🎓 OBSERVER PATTERN LÀ GÌ?
 * Subject (Broadcaster) giữ danh sách các Observers (ClientHandlers).
 * Khi trạng thái thay đổi (có Bid mới), Subject tự động gọi onUpdate()
 * trên tất cả các Observers đăng ký.
 *
 * Tính đa luồng (Thread-safety):
 *   - ConcurrentHashMap: nhiều thread đọc/ghi key an toàn.
 *   - CopyOnWriteArrayList: tối ưu cho tình huống ít thread ghi (đăng ký),
 *     nhưng có RẤT NHIỀU thread đọc (broadcast bid). Tránh ConcurrentModificationException.
 * ============================================================
 */
public class AuctionBroadcaster {

  // Map: SessionId -> Danh sách các Client đang xem phiên đấu giá đó
  private final Map<String, List<ClientObserver>> sessionSubscribers = new ConcurrentHashMap<>();

  // Singleton
  private static final AuctionBroadcaster instance = new AuctionBroadcaster();

  private AuctionBroadcaster() {}

  public static AuctionBroadcaster getInstance() {
    return instance;
  }

  public List<ClientObserver> getSubscribersForSession(String sessionId) {
    return sessionSubscribers.get(sessionId);
  }

  /**
   * Client mở màn hình đấu giá -> Đăng ký nhận thông báo.
   */
  public void subscribe(String sessionId, ClientObserver observer) {
    sessionSubscribers
        .computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>())
        .add(observer);
    System.out.println("[Broadcaster] User " + observer.getUserId() + " theo dõi phiên " + sessionId.substring(0, 8));
  }

  /**
   * Client đóng màn hình hoặc ngắt kết nối -> Hủy đăng ký.
   */
  public void unsubscribe(String sessionId, ClientObserver observer) {
    List<ClientObserver> list = sessionSubscribers.get(sessionId);
    if (list != null) {
      list.remove(observer);
      System.out.println("[Broadcaster] User " + observer.getUserId() + " ngừng theo dõi phiên " + sessionId.substring(0, 8));
    }
  }

  /**
   * Gỡ đăng ký của client ở mọi phiên (khi mất kết nối đột ngột).
   */
  public void unsubscribeAll(ClientObserver observer) {
    for (String sessionId : sessionSubscribers.keySet()) {
      unsubscribe(sessionId, observer);
    }
  }

  /**
   * Phát đi một gói Response cho tất cả những người đang theo dõi phiên.
   */
  public void broadcastToSession(String sessionId, Response eventResponse) {
    List<ClientObserver> list = sessionSubscribers.get(sessionId);
    if (list != null && !list.isEmpty()) {
      for (ClientObserver observer : list) {
        try {
            // Có thể tối ưu: chạy trong ThreadPool nếu muốn gửi qua mạng không block
            observer.onUpdate(eventResponse);
        } catch (Exception e) {
            System.err.println("[Broadcaster] Lỗi gửi broadcast cho " + observer.getUserId() + ": " + e.getMessage());
        }
      }
      System.out.println("[Broadcaster] Đã broadcast tới " + list.size() + " client cho phiên " + sessionId.substring(0, 8));
    }
  }
}
