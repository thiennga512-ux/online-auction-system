package com.auction.model;

import com.auction.service.Observer;

// Bidder kế thừa User VÀ triển khai interface Observer để nhận thông báo
public class Bidder extends User implements Observer {
    private double balance; // Số dư tiền trong tài khoản (Thuộc tính riêng của người mua)

    public Bidder(String id, String username, String email, String password,double balance) {
        super(id, username, email,password); // Gọi hàm khởi tạo của lớp cha (User)
        this.balance = balance;
    }

    // Hành động đặt giá
    public void placeBid(double amount) {
        System.out.println(">>> " + username + " quyết định đặt giá: " + amount + " VNĐ");
    }

    // Phương thức bắt buộc phải có khi implement Observer
    // Sẽ được gọi tự động khi phiên đấu giá có giá mới
    @Override
    public void update(String message) {
        System.out.println("[THÔNG BÁO tới " + username + "]: " + message);
    }

    public double getBalance() {
        return balance;
    }
}