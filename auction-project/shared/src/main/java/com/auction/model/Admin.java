package com.auction.model;

import java.time.LocalDateTime;
import com.auction.enums.UserRole;

public class Admin extends User {

    // Constructor cho tạo mới Admin (thường tạo qua script hoặc hệ thống nội bộ)
    public Admin(String username, String passwordHash, String email, String fullName) {
        super(username, passwordHash, email, fullName, UserRole.ADMIN);
    }

    // Constructor nạp từ MySQL
    public Admin(String id, LocalDateTime createdAt, LocalDateTime updateAt, String username, 
                 String passwordHash, String email, String fullName, boolean active) {
        super(id, createdAt, updateAt, username, passwordHash, email, fullName, UserRole.ADMIN, active);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    @Override
    public String getDashboardView() {
        return "/views/admin_dashboard.fxml";
    }

    @Override
    public void printInfo() {
        System.out.printf("[QUẢN TRỊ VIÊN] ID=%s | Tên: %s | Email: %s%n", 
                          getId(), getFullName(), getEmail());
    }

}