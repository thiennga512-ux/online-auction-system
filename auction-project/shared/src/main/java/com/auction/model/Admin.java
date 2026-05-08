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

    /**
     * Lưu ý: Các phương thức dưới đây nên được triển khai logic thực tế trong AdminService.
     * Ở đây chúng ta định nghĩa các quyền hạn đặc trưng của Admin.
     */

    // 1. Quyền khóa/mở tài khoản người dùng
    // Admin sẽ thay đổi trạng thái 'active' của bất kỳ User nào
    public void toggleUserStatus(User targetUser) {
        targetUser.setActive(!targetUser.isActive());
    }

    @Override
    public void printInfo() {
        System.out.printf("[QUẢN TRỊ VIÊN] ID=%s | Tên: %s | Email: %s%n", 
                          getId(), getFullName(), getEmail());
    }
}