package com.auction.model;

import com.auction.service.Auction;
import java.time.LocalDateTime;

/**
 * Admin: Quản trị viên hệ thống.
 */
public class Admin extends User {

    public Admin(String id, String fullName, String username, String email, String password, String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt, boolean active) {
        super(id, fullName, username, email, password, phoneNumber, gender, dateOfBirth, createdAt, active);
    }

    public Admin(String fullName, String username, String email, String password, String gender, String dateOfBirth) {
        super(fullName, username, email, password, gender, dateOfBirth);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    @Override
    public String getDashboardView() {
        return "--- GIAO DIỆN QUẢN TRỊ VIÊN ---";
    }

    // 🔥 Khóa người dùng
    public void banUser(User user) {
        user.setActive(false);
        System.out.println("Admin đã khóa tài khoản: " + user.getUsername());
    }

    // 🔥 Hủy phiên đấu giá
    public void cancelAuction(Auction auction) {
        auction.closeAuction();
        System.out.println("Admin đã hủy đấu giá sản phẩm: " + auction.getItem().getName());
    }
}