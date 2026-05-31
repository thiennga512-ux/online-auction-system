package com.auction.server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.network.Response;
import com.auction.server.network.ClientHandler;
import com.auction.server.network.UserConnectionManager;
import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;

/**
 * NotificationService — Gửi thông báo chuông qua Socket
 *
 * - Seller: nhận thông báo cho phiên của mình (tự động)
 * - Bidder: nhận thông báo khi đã "đăng ký" nhận cho phiên đó
 * - Người đặt giá KHÔNG nhận thông báo chính mình vừa bid
 */
public class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();

    /**
     * Map: sessionId → Set of userIds đã đăng ký nhận thông báo cho phiên đó
     */
    private final Map<String, Set<String>> registrations = new ConcurrentHashMap<>();

    private NotificationService() {
    }

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------
    // ĐĂNG KÝ / HỦY ĐĂNG KÝ THÔNG BÁO
    // -------------------------------------------------------

    /**
     * Bidder đăng ký nhận thông báo chuông cho một phiên đấu giá.
     * Mỗi lần có bid mới trong phiên đó, chuông sẽ rung với người đã đăng ký
     * (trừ khi chính họ vừa đặt giá).
     */
    public void registerNotification(String sessionId, String userId) {
        registrations.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        System.out.printf("[NotificationService] User %s đăng ký chuông cho phiên %s%n",
                userId.substring(0, 8), sessionId.substring(0, 8));
    }

    /**
     * Lấy danh sách userId đã đăng ký nhận thông báo cho phiên (để debug/kiểm tra).
     */
    public List<String> getRegisteredUsers(String sessionId) {
        Set<String> set = registrations.get(sessionId);
        return set == null ? List.of() : List.copyOf(set);
    }

    // -------------------------------------------------------
    // PHIÊN BẮT ĐẦU
    // -------------------------------------------------------

    /**
     * Gửi thông báo chuông cho Seller khi phiên đấu giá bắt đầu.
     */
    public void notifyAuctionStarted(AuctionSession session) {
        String message = "Phiên đấu giá sản phẩm '" + session.getItem().getName()
                + "' của bạn đã bắt đầu!";
        pushBellNotification(session.getSellerId(), session.getId(), message);

        // Thông báo cho Bidder đã đăng ký
        String bidderMsg = "Phiên đấu giá " + session.getItem().getName() + "' đã bắt đầu! Hãy vào đặt giá ngay.";
        pushToRegistered(session.getId(), bidderMsg, session.getSellerId(), null);

        System.out.printf("[NotificationService]Gửi chuông cho Seller + Bidder đã đk — Phiên %s bắt đầu%n",
                session.getId().substring(0, 8));
    }

    // -------------------------------------------------------
    // PHIÊN KẾT THÚC
    // -------------------------------------------------------

    /**
     * Gửi thông báo chuông cho Seller khi phiên đấu giá kết thúc.
     * Nếu có người thắng: thông báo bán thành công + số tiền nhận được.
     * Nếu không có người thắng: thông báo phiên kết thúc không có giao dịch.
     */
    public void notifyAuctionEnded(AuctionSession session) {
        String sellerMessage;
        String bidderMessage;

        if (session.getCurrentWinnerId() != null) {
            double finalPrice = session.getCurrentPrice();
            double commission = finalPrice * 0.05;
            double sellerReceives = finalPrice - commission;
            sellerMessage = "Sản phẩm '" + session.getItem().getName() + "' đã được bán thành công!"
                    + " Người mua: " + session.getCurrentWinnerName()
                    + " | Giá: " + String.format("%,.0f", finalPrice) + " VND"
                    + " | Bạn nhận: " + String.format("%,.0f", sellerReceives) + " VND (sau 5% hoa hồng)";
            bidderMessage = "Phiên đấu giá '" + session.getItem().getName() + "' đã kết thúc."
                    + " Người thắng: " + session.getCurrentWinnerName()
                    + " | Giá: " + String.format("%,.0f", finalPrice) + " VND";
        } else {
            sellerMessage = " Phiên đấu giá sản phẩm '" + session.getItem().getName()
                    + "' đã kết thúc mà không có người đặt giá.";
            bidderMessage = "Phiên đấu giá '" + session.getItem().getName()
                    + "' đã kết thúc mà không có người đặt giá.";
        }

        pushBellNotification(session.getSellerId(), session.getId(), sellerMessage);
        // Thông báo cho winner nếu có
        if (session.getCurrentWinnerId() != null) {
            String winnerMsg = "Chúc mừng! Bạn đã thắng phiên đấu giá '" + session.getItem().getName()
                    + "' với giá " + String.format("%,.0f", session.getCurrentPrice()) + " VND!";
            pushBellNotification(session.getCurrentWinnerId(), session.getId(), winnerMsg);
        }
        // Thông báo cho Bidder khác đã đăng ký
        pushToRegistered(session.getId(), bidderMessage, session.getSellerId(), session.getCurrentWinnerId());

        System.out.printf("[NotificationService] Gửi chuông cho Seller + Bidder đã đk — Phiên %s kết thúc%n",
                session.getId().substring(0, 8));

        // Dọn sạch danh sách đăng ký
        registrations.remove(session.getId());
    }

    // -------------------------------------------------------
    // BID MỚI — KHÔNG gửi cho chính người vừa bid
    // -------------------------------------------------------

    /**
     * Gửi thông báo chuông cho Seller khi có bid mới trong phiên của họ.
     * Gửi cho Bidder đã đăng ký, TRỪ người vừa đặt giá (bidderId).
     */
    public void notifyNewBidPlaced(AuctionSession session, Bid bid) {
        // Seller nhận thông báo (chỉ khi Seller không phải là người bid - an toàn)
        String sellerMsg = "Có bid mới trong phiên '" + session.getItem().getName() + "'!"
                + " Giá hiện tại: " + String.format("%,.0f", bid.getAmount()) + " VND"
                + " bởi " + bid.getBidderName() + ".";
        pushBellNotification(session.getSellerId(), session.getId(), sellerMsg);

        // Bidder đã đăng ký nhận thông báo (trừ người vừa bid và Seller)
        String bidderMsg = "Có bid mới trong phiên '" + session.getItem().getName() + "'!"
                + " Giá: " + String.format("%,.0f", bid.getAmount()) + " VND bởi " + bid.getBidderName() + ".";
        pushToRegistered(session.getId(), bidderMsg, session.getSellerId(), bid.getBidderId());
    }

    // -------------------------------------------------------
    // HELPER — Gửi chuông đến tất cả Bidder đã đăng ký (trừ excludeIds)
    // -------------------------------------------------------

    private void pushToRegistered(String sessionId, String message, String... excludeIds) {
        Set<String> registered = registrations.get(sessionId);
        if (registered == null || registered.isEmpty())
            return;

        Set<String> excluded = Set.of(excludeIds);
        for (String userId : registered) {
            if (!excluded.contains(userId)) {
                pushBellNotification(userId, sessionId, message);
            }
        }
    }

    // -------------------------------------------------------
    // HELPER — Gửi chuông qua socket
    // -------------------------------------------------------

    private void pushBellNotification(String userId, String sessionId, String message) {
        if (userId == null)
            return;
        ClientHandler handler = UserConnectionManager.getInstance().getHandler(userId);
        if (handler != null) {
            Dto.NotificationEvent event = new Dto.NotificationEvent(
                    sessionId,
                    message,
                    LocalDateTime.now().toString());
            Response response = Response.success(
                    ActionType.GLOBAL_NOTIFICATION_BROADCAST, message, event);
            handler.sendResponse(response);
            System.out.printf("[NotificationService]Đã gửi chuông đến user %s (online)%n",
                    userId.substring(0, 8));
        } else {
            System.out.printf("[NotificationService]User %s offline — bỏ qua thông báo chuông%n",
                    userId.substring(0, 8));
        }
    }

    public void notifyAutoBidFired(
            String userId,
            String sessionId,
            String message) {

        System.out.println(
                "[AUTO BID NOTIFY] " + message);
    }

    public void notifyDepositApproved(String userId, double amount) {
        String message = "Yêu cầu nạp tiền " + String.format("%,.0f", amount)
                + " VND của bạn đã được duyệt thành công!";
        pushBellNotification(userId, "SYSTEM", message);
    }
}
