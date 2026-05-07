package com.auction.model;

<<<<<<< HEAD
import java.time.LocalDateTime;
import model.enums.UserRole;

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
=======
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
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
    }
}