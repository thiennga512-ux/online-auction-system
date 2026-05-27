package com.auction.dto;

//Class datatransferobjects chua cac lop record de chuyen du lieu giua client va server
public class Dto {

        // --- Requests ---

        public record LoginRequest(String email, String password) {
        }

        public record RegisterUserRequest(
                        String fullName, String username, String email, String password, String gender,
                        String dateOfBirth) {
        }

        public record UpgradeToSellerRequest(String shopName, String citizenId) {
        }

        public record PlaceBidRequest(String sessionId, double amount) {
        }

        public record SubscribeRequest(String sessionId) {
        }

        public record RegisterNotificationRequest(String sessionId) {
        }

        public record ListItemRequest(
                        String name, String description, double startingPrice, double minIncrement, String imageUrl,
                        String category, String brand, String model, int warrantyMonths, String conditionStr,
                        String artistName, Integer creationYear, String medium, Boolean authenticated,
                        String certificateId,
                        String dimensions,
                        String vehicleType, String make, Integer year, Integer mileage, String fuelType,
                        String transmission,
                        String color, String licensePlate, Boolean hasValidRegistry) {
        }

        public record CreateAuctionRequest(
                        String itemId, String startTime, String endTime, int antiSnipingSeconds) {
        }

        public record ApproveAuctionRequest(String sessionId) {
        }

        public record RejectAuctionRequest(String sessionId, String reason) {
        }

        public record CancelAuctionRequest(String sessionId, String reason) {
        }

        /** Admin khoá / mở khoá tài khoản */
        public record SetUserActiveRequest(String targetUserId, boolean active) {
        }

        /** Admin nạp tiền cho Bidder */
        public record DepositRequest(String bidderId, double amount) {
        }

        public record AutoBidConfigRequest(
                        String sessionId,
                        double maxBudget,
                        double customIncrement) {
        }

        // --- Phản hồi (Responses) chứa dữ liệu ---

        /** Khi có người đặt giá mới, Server broadcast sự kiện này về */
        public record NewBidEvent(
                        String sessionId,
                        String bidId,
                        String bidderId,
                        String bidderName,
                        double amount,
                        String timestamp,
                        String bidType) {
        }

        /** Thông báo gửi tới chuông toàn cục */
        public record NotificationEvent(
                        String sessionId,
                        String message,
                        String timestamp) {
        }

        /** Khi phiên đấu giá bắt đầu */
        public record AuctionStartedEvent(
                        String sessionId,
                        double startPrice) {
        }

        /** Khi phiên đấu giá kết thúc */
        public record AuctionEndedEvent(
                        String sessionId,
                        String winnerName,
                        double finalPrice) {
        }

        /** Trả về thông tin cơ bản của User khi login thành công */
        public record UserProfileResponse(
                        String id,
                        String fullName,
                        String role,
                        double balance // Dùng chung cho deposit (Bidder) hoặc balance (Seller)
        ) {
        }

        /** Trả về số dư mới sau khi nạp tiền hoặc đồng bộ */
        public record DepositResultResponse(
                        double newBalance) {
        }

        public record ItemResponse(
                        String id,
                        String name,
                        String description,
                        double startingPrice,
                        String status, // AVAILABLE, IN_AUCTION, SOLD
                        String imageUrl) {
        }

        /** Tóm tắt thông tin User trả về cho Admin quản lý */
        public record UserSummaryResponse(
                        String id,
                        String fullName,
                        String email,
                        String role,
                        boolean active) {
        }

        /** Tóm tắt phiên đấu giá trả về cho Seller xem danh sách phiên của mình */
        public record AuctionSessionResponse(
                        String id,
                        String itemName,
                        double currentPrice,
                        String status,
                        String startTime,
                        String endTime) {
        }

        /**
         * DTO đầy đủ cho màn hình Home (thay thế việc gửi AuctionSession trực tiếp).
         * Tránh vấn đề GSON không deserialize được abstract class Item.
         */
        public record AuctionCardDto(
                        String sessionId,
                        String itemId,
                        String itemName,
                        String itemDescription,
                        double startingPrice,
                        double minIncrement,
                        double currentPrice,
                        String currentWinnerId,
                        String currentWinnerName,
                        String sellerName,
                        String status,
                        String startTime,
                        String actualEndTime,
                        int antiSnipingSeconds,
                        String imageUrl,
                        String category,
                        java.util.Map<String, String> itemAttributes) {

                public Object technicalSummary() {
                        throw new UnsupportedOperationException("Unimplemented method 'technicalSummary'");
                }
        }

        /** Thông tin một yêu cầu nạp tiền đang chờ duyệt (gửi về Admin) */
        public record PendingDepositDto(
                        String requestId,
                        String bidderId,
                        String bidderName,
                        String bidderEmail,
                        double amount,
                        String requestedAt) {
        }

        public record ApproveDepositRequest(String requestId) {
        }

        /** Admin từ chối một yêu cầu nạp tiền */
        public record RejectDepositRequest(String requestId, String reason) {
        }

        public record AuctionResultDto(
                        String endTime,
                        String auctionName,
                        String status, // SUCCESS / FAILED
                        String reason) {
        }
}
