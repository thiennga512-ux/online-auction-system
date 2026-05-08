package com.auction.service.auction;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

/**
 * Quản lý tập trung tất cả các phiên đấu giá đang diễn ra trên Server.
 * Áp dụng Singleton Pattern để đảm bảo duy nhất một bộ quản lý.
 */
public class AuctionManager {
    
    // Sử dụng Singleton Pattern
    private static AuctionManager instance;
    
    // ConcurrentHashMap để đảm bảo an toàn khi nhiều Thread (Socket) cùng truy cập
    // Key: ID của phiên đấu giá (String), Value: Đối tượng AuctionSession
    private final Map<String, AuctionSession> activeAuctions;

    private AuctionManager() {
        this.activeAuctions = new ConcurrentHashMap<>();
    }

    /**
     * Lấy instance duy nhất của AuctionManager.
     */
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    /**
     * Đưa một phiên đấu giá mới vào danh sách quản lý.
     * @param session Phiên đấu giá đã được khởi tạo.
     */
    public void addSession(AuctionSession session) {
        if (session != null && session.getId() != null) {
            activeAuctions.put(session.getId(), session);
            System.out.println("[Manager] Đã thêm phiên đấu giá mới: " + session.getId());
        }
    }

    /**
     * Tìm kiếm một phiên đấu giá dựa trên ID.
     * Thường dùng khi Client gửi Request đặt giá kèm theo SessionID.
     */
    public AuctionSession getSession(String sessionId) {
        return activeAuctions.get(sessionId);
    }

    /**
     * Lấy danh sách tất cả các phiên đấu giá đang hoạt động.
     * Dùng để gửi danh sách về cho Client hiển thị lên UI.
     */
    public Collection<AuctionSession> getAllSessions() {
        return activeAuctions.values();
    }

    /**
     * Loại bỏ một phiên đấu giá khỏi danh sách (khi đã kết thúc hoặc bị hủy).
     */
    public void removeSession(String sessionId) {
        activeAuctions.remove(sessionId);
        System.out.println("[Manager] Đã đóng phiên đấu giá: " + sessionId);
    }

    /**
     * Kiểm tra xem một phiên đấu giá có đang tồn tại không.
     */
    public boolean exists(String sessionId) {
        return activeAuctions.containsKey(sessionId);
    }
}