package com.auction.model;

// Lớp trừu tượng User: Dùng làm khuôn mẫu cho các loại người dùng khác.
// Không ai được phép tạo trực tiếp 1 "User" chung chung (không thể dùng: new User())
public abstract class User extends Entity {
    protected String username;// Dùng protected để các lớp con (Bidder, Seller) có thể dùng chung
    protected String email;
    protected String password;
    

    // Constructor (Hàm khởi tạo)
    public User(String id,String username, String email,String password) {
        super(id);
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Phương thức chung cho mọi User
    public void login() {
        System.out.println(username + " đã đăng nhập vào hệ thống.");
    }

    public void logout() {
        System.out.println(username + " đã đăng xuất.");
    }

    // Getter để lấy tên hiển thị
    public String getUsername() {
        return username;
    }
}