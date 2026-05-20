package com.auction.server.service;

import com.auction.common.model.auction.AuctionSession;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDAO;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationService {
    
    private static NotificationService instance;
    
    // Lưu trữ mapping: sessionId -> Set(userId)
    private final Map<String, Set<String>> subscribers = new ConcurrentHashMap<>();
    private UserDAO userDAO;

    private NotificationService() {}

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public void init(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void registerNotification(String sessionId, String userId) {
        subscribers.computeIfAbsent(sessionId, k -> new HashSet<>()).add(userId);
        System.out.println("[NotificationService] User " + userId + " đăng ký nhận thông báo cho phiên " + sessionId);
    }

    public void notifyAuctionStarted(AuctionSession session) {
        if (userDAO == null) return;
        
        // Gửi email cho Seller
        try {
            Optional<User> sellerOpt = userDAO.findById(session.getSellerId());
            if (sellerOpt.isPresent()) {
                User seller = sellerOpt.get();
                String subject = "Phiên đấu giá của bạn đã bắt đầu: " + session.getItem().getName();
                String body = "Xin chào " + seller.getFullName() + ",\n\n"
                        + "Phiên đấu giá cho sản phẩm '" + session.getItem().getName() + "' của bạn đã bắt đầu mở nhận giá.\n"
                        + "Giá khởi điểm: " + String.format("%,.0f", session.getItem().getBasePrice()) + " VND.\n\n"
                        + "Trân trọng,\nBan quản trị hệ thống";
                EmailService.sendEmailAsync(seller.getEmail(), subject, body);
            }
        } catch (Exception e) {
            System.err.println("[NotificationService] Lỗi gửi email cho Seller: " + e.getMessage());
        }

        // Gửi email cho các Bidder đã đăng ký
        Set<String> userIds = subscribers.getOrDefault(session.getId(), new HashSet<>());
        for (String userId : userIds) {
            try {
                Optional<User> userOpt = userDAO.findById(userId);
                if (userOpt.isPresent()) {
                    User bidder = userOpt.get();
                    String subject = "Phiên đấu giá bạn quan tâm đã bắt đầu: " + session.getItem().getName();
                    String body = "Xin chào " + bidder.getFullName() + ",\n\n"
                            + "Phiên đấu giá cho sản phẩm '" + session.getItem().getName() + "' mà bạn đăng ký nhận thông báo hiện ĐÃ BẮT ĐẦU.\n"
                            + "Nhanh tay vào hệ thống để theo dõi và đặt giá nhé!\n\n"
                            + "Trân trọng,\nBan quản trị hệ thống";
                    EmailService.sendEmailAsync(bidder.getEmail(), subject, body);
                }
            } catch (Exception e) {
                System.err.println("[NotificationService] Lỗi gửi email cho Bidder: " + e.getMessage());
            }
        }
    }

    public void notifyAuctionEnded(AuctionSession session) {
        if (userDAO == null) return;
        
        boolean hasWinner = session.getCurrentWinnerId() != null;
        
        // Gửi email cho Seller (Nếu KHÔNG có người thắng. Vì nếu có, AuctionService đã gửi rồi)
        if (!hasWinner) {
            try {
                Optional<User> sellerOpt = userDAO.findById(session.getSellerId());
                if (sellerOpt.isPresent()) {
                    User seller = sellerOpt.get();
                    String subject = "Phiên đấu giá kết thúc không thành công: " + session.getItem().getName();
                    String body = "Xin chào " + seller.getFullName() + ",\n\n"
                            + "Phiên đấu giá cho sản phẩm '" + session.getItem().getName() + "' đã kết thúc mà không có người trả giá thành công.\n"
                            + "Vui lòng xem lại thông tin sản phẩm và có thể mở lại phiên mới sau.\n\n"
                            + "Trân trọng,\nBan quản trị hệ thống";
                    EmailService.sendEmailAsync(seller.getEmail(), subject, body);
                }
            } catch (Exception e) {
                 System.err.println("[NotificationService] Lỗi: " + e.getMessage());
            }
        }
        
        // Gửi email cho các Bidder đã đăng ký (trừ người thắng, vì AuctionService đã gửi cho winner)
        Set<String> userIds = subscribers.getOrDefault(session.getId(), new HashSet<>());
        for (String userId : userIds) {
            if (hasWinner && userId.equals(session.getCurrentWinnerId())) {
                continue; // Bỏ qua người thắng
            }
            try {
                Optional<User> userOpt = userDAO.findById(userId);
                if (userOpt.isPresent()) {
                    User bidder = userOpt.get();
                    String subject = "Phiên đấu giá kết thúc: " + session.getItem().getName();
                    String body = "Xin chào " + bidder.getFullName() + ",\n\n"
                            + "Phiên đấu giá cho sản phẩm '" + session.getItem().getName() + "' mà bạn quan tâm đã kết thúc.\n"
                            + (hasWinner ? "Người chiến thắng: " + session.getCurrentWinnerName() + " với giá " + String.format("%,.0f", session.getCurrentPrice()) + " VND.\n\n"
                                         : "Rất tiếc, phiên đấu giá không có ai thắng cuộc.\n\n")
                            + "Trân trọng,\nBan quản trị hệ thống";
                    EmailService.sendEmailAsync(bidder.getEmail(), subject, body);
                }
            } catch (Exception e) {
                System.err.println("[NotificationService] Lỗi: " + e.getMessage());
            }
        }
    }

    public void notifyNewBidPlaced(AuctionSession session, com.auction.common.model.auction.Bid bid) {
        if (userDAO == null) return;
        
        Set<String> userIds = subscribers.getOrDefault(session.getId(), new HashSet<>());
        for (String userId : userIds) {
            // Không gửi thông báo cho chính người vừa đặt giá
            if (userId.equals(bid.getBidderId())) {
                continue;
            }
            try {
                Optional<User> userOpt = userDAO.findById(userId);
                if (userOpt.isPresent()) {
                    User bidder = userOpt.get();
                    String subject = "Có trả giá mới: " + session.getItem().getName();
                    String body = "Xin chào " + bidder.getFullName() + ",\n\n"
                            + "Sản phẩm '" + session.getItem().getName() + "' mà bạn quan tâm vừa có lượt trả giá mới.\n"
                            + "Người trả giá: " + bid.getBidderName() + "\n"
                            + "Mức giá mới: " + String.format("%,.0f", bid.getAmount()) + " VND.\n\n"
                            + "Nhanh tay vào hệ thống để theo dõi và đặt giá nếu bạn muốn sở hữu sản phẩm này!\n\n"
                            + "Trân trọng,\nBan quản trị hệ thống";
                    // EmailService.sendEmailAsync(bidder.getEmail(), subject, body);
                    
                    // Gửi qua WebSocket tới chuông thông báo
                    com.auction.server.network.ClientHandler handler = com.auction.server.network.UserConnectionManager.getInstance().getHandler(userId);
                    if (handler != null) {
                        String notifMsg = "Sản phẩm '" + session.getItem().getName() + "' vừa có lượt trả giá mới!\nGiá mới: " + String.format("%,.0f", bid.getAmount()) + " VND";
                        com.auction.common.dto.Dto.NotificationEvent event = new com.auction.common.dto.Dto.NotificationEvent(session.getId(), notifMsg, bid.getTimestamp().toString());
                        handler.sendResponse(com.auction.common.network.Response.success(com.auction.common.network.ActionType.GLOBAL_NOTIFICATION_BROADCAST, event));
                    }
                }
            } catch (Exception e) {
                System.err.println("[NotificationService] Lỗi gửi email cho Bidder (New Bid): " + e.getMessage());
            }
        }
    }
}
