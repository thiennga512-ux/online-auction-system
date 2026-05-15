package com.auction.server.service;

import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;

public class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();

    private NotificationService() {}

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    public void notifyAuctionStarted(AuctionSession session) {
        System.out.println("[NotificationService] Thông báo phiên đấu giá bắt đầu: " + session.getId());
    }

    public void notifyAuctionEnded(AuctionSession session) {
        System.out.println("[NotificationService] Thông báo phiên đấu giá kết thúc: " + session.getId());
    }

    public void notifyNewBidPlaced(AuctionSession session, Bid bid) {
        System.out.println("[NotificationService] Thông báo có bid mới: " + bid.getAmount());
    }

    public void registerNotification(String sessionId, String userId) {
        System.out.println("[NotificationService] User " + userId + " đăng ký nhận thông báo cho phiên: " + sessionId);
        // Note: Real implementation would save this to DB or a list if needed.
    }
}
