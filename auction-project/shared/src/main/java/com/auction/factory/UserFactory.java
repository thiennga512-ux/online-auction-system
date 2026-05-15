package com.auction.factory;

import com.auction.model.*;

import com.auction.enums.UserRole;
import java.time.LocalDateTime;

/**
 * Class UserFactory — Chịu trách nhiệm khởi tạo các đối tượng User.
 * Giúp tách biệt logic tạo mới (cho Client) và tái tạo (cho Database).
 */
public class UserFactory {

    private UserFactory() {
        throw new UnsupportedOperationException("UserFactory là Utility Class");
    }

    // ============================================================
    // 1. LUỒNG TẠO MỚI (Dùng cho Đăng ký / Socket Client)
    // ============================================================

    /**
     * Tạo User mới dựa trên UserRole. Dùng cho luồng đăng ký.
     */
    public static User create(UserRole role, String username, String passwordHash, String email, String fullName) {
        return switch (role) {
            case ADMIN -> new Admin(username, passwordHash, email, fullName);
            case SELLER -> new Seller(username, passwordHash, email, fullName, 0.0,0, "",""); // Mặc định rating 0.0
            case BIDDER -> new Bidder(username, passwordHash, email, fullName);
        };
    }

    /**
     * Overload nhận String role (tiện khi nhận dữ liệu thô từ JSON).
     */
    public static User create(String roleStr, String username, String passwordHash, String email, String fullName) {
        UserRole role = UserRole.fromString(roleStr);
        return create(role, username, passwordHash, email, fullName);
    }

    // ============================================================
    // 2. LUỒNG TÁI TẠO TỪ DATABASE (Dùng cho UserDAO)
    // ============================================================

    /**
     * Tái tạo đối tượng Bidder với đầy đủ thông tin tài chính từ DB.
     */
    public static Bidder rebuildBidder(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
            String username, String passwordHash, String email, String fullName,
            boolean active, double balance, double frozenBalance, String address) {
        return new Bidder(id, createdAt, updatedAt, username, passwordHash, email, fullName, active, balance,
                frozenBalance, address);
    }

    /**
     * Tái tạo đối tượng Seller với thông tin doanh thu và rating từ DB.
     */
    public static Seller rebuildSeller(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
            String username, String passwordHash, String email, String fullName,
            boolean active, double balance, double rating, int ratingCount, String shopName, String citizenId) {
        return new Seller(id, createdAt, updatedAt, username, passwordHash, email, fullName, active, balance, rating,
                ratingCount, shopName, citizenId);
    }

    /**
     * Tái tạo đối tượng Admin từ DB.
     */
    public static Admin rebuildAdmin(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
            String username, String passwordHash, String email, String fullName, boolean active) {
        return new Admin(id, createdAt, updatedAt, username, passwordHash, email, fullName, active);
    }

    /**
     * Phương thức tổng quát để UserDAO gọi khi query bảng users.
     * Tùy vào giá trị cột 'role' trong DB mà hàm này sẽ gọi đến hàm rebuild tương
     * ứng.
     */
    public static User reconstruct(String roleStr, String id, LocalDateTime createdAt, LocalDateTime updatedAt,
            String username, String passwordHash, String email, String fullName,
            boolean active, double balance, double frozenBalance, String address, 
            double rating, int ratingCount, String shopName, String citizenId) {

        UserRole role = UserRole.fromString(roleStr);

        switch (role) {
            case ADMIN:
                return rebuildAdmin(id, createdAt, updatedAt, username, passwordHash, email, fullName, active);
            case SELLER:
                return rebuildSeller(id, createdAt, updatedAt, username, passwordHash, email, fullName, active,
                        balance, rating, ratingCount, shopName, citizenId);
            case BIDDER:
                return rebuildBidder(id, createdAt, updatedAt, username, passwordHash, email, fullName, active,
                        balance, frozenBalance, address);
            default:
                throw new IllegalArgumentException("Unknown role: " + roleStr);
        }
    }
}
