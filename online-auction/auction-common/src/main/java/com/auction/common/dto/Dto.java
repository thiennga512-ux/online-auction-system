package com.auction.common.dto;

/**
 * ============================================================
 * Class DataTransferObjects (DTOs)
 * ============================================================
 * Dùng Java 17 Records để định nghĩa các payload gửi qua mạng.
 * Record là immutable data class (chỉ có data, không có logic).
 * ============================================================
 */
public class Dto {

  // --- Requests ---

  public record LoginRequest(String email, String password) {}

  public record RegisterUserRequest(
      String fullName, String username, String email, String password,
      String gender, String dateOfBirth) {}

  public record UpgradeToSellerRequest(String shopName, String citizenId) {}

  public record PlaceBidRequest(String sessionId, double amount) {}

  public record SubscribeRequest(String sessionId) {}
  
  public record RegisterNotificationRequest(String sessionId) {}

  public record ListItemRequest(
      String name, String description, double basePrice, double minIncrement,
      String category, String imageUrl,
      // Electronics
      String brand, String model, int warrantyMonths, String conditionStr,
      // Art
      String artistName, int creationYear, String medium, boolean authenticated, String certificateId, String dimensions,
      // Vehicle
      String vehicleType, String make, int year, double mileage, String fuelType, String transmission, String color, String licensePlate, boolean hasValidRegistry
  ) {}

  public record CreateAuctionRequest(
      String itemId, String startTime, String endTime, int antiSnipingSeconds
  ) {}

  public record ApproveAuctionRequest(String sessionId) {}

  public record RejectAuctionRequest(String sessionId, String reason) {}
  
  public record CancelAuctionRequest(String sessionId) {}

  /** Admin khoá / mở khoá tài khoản */
  public record SetUserActiveRequest(String targetUserId, boolean active) {}

  /** Admin nạp tiền cho Bidder */
  public record DepositRequest(String bidderId, double amount) {}

  /** Bidder tự nạp tiền vào tài khoản của mình */
  public record SelfDepositRequest(double amount) {}

  /** Server trả về sau khi nạp tiền thành công (có số dư mới) */
  public record DepositResultResponse(double newBalance) {}

  public record AutoBidConfigRequest(
      String sessionId,
      double maxBudget,
      String strategyType // AGGRESSIVE, CONSERVATIVE
  ) {}

  // --- Phản hồi (Responses) chứa dữ liệu ---

  /** Khi có người đặt giá mới, Server broadcast sự kiện này về */
  public record NewBidEvent(
      String sessionId,
      String bidId,
      String bidderId,
      String bidderName,
      double amount,
      String timestamp
  ) {}

  /** Thông báo gửi tới chuông toàn cục */
  public record NotificationEvent(
      String sessionId,
      String message,
      String timestamp
  ) {}

  /** Khi phiên đấu giá bắt đầu */
  public record AuctionStartedEvent(
      String sessionId,
      double startPrice
  ) {}

  /** Khi phiên đấu giá kết thúc */
  public record AuctionEndedEvent(
      String sessionId,
      String winnerName,
      double finalPrice
  ) {}

  /** Trả về thông tin cơ bản của User khi login thành công */
  public record UserProfileResponse(
      String id,
      String fullName,
      String role,
      double balance // Dùng chung cho deposit (Bidder) hoặc balance (Seller)
  ) {}

  public record ItemResponse(
      String id,
      String name,
      String description,
      double basePrice,
      String status, // AVAILABLE, IN_AUCTION, SOLD
      String imageUrl
  ) {}

  /** Tóm tắt thông tin User trả về cho Admin quản lý */
  public record UserSummaryResponse(
      String id,
      String fullName,
      String email,
      String role,
      boolean active
  ) {}

  /** Tóm tắt phiên đấu giá trả về cho Seller xem danh sách phiên của mình */
  public record AuctionSessionResponse(
      String id,
      String itemName,
      double currentPrice,
      String status,
      String startTime,
      String endTime
  ) {}

  /**
   * DTO đầy đủ cho màn hình Home (thay thế việc gửi AuctionSession trực tiếp).
   * Tránh vấn đề GSON không deserialize được abstract class Item.
   */
  public record AuctionCardDto(
      String sessionId,
      String itemId,
      String itemName,
      String itemDescription,
      double basePrice,
      double minIncrement,
      double currentPrice,
      String currentWinnerId,
      String currentWinnerName,
      String sellerName,
      String status,
      String startTime,
      String actualEndTime,
      int antiSnipingSeconds,
      String itemCategory,
      String imageUrl
  ) {}
}