package com.auction.model;

import com.auction.service.Auction;

class Admin extends User {

    public Admin(String id, String username, String password, String email) {
        super(id, username, password, email);
    }

    // 🔥 khóa user
    public void banUser(User user) {
        System.out.println("Admin đã khóa tài khoản: " + user.getUsername());
    }

    // 🔥 hủy phiên đấu giá
    public void cancelAuction(Auction auction) {
        System.out.println("Admin đã hủy đấu giá: " + auction.getItem().getName());
    }
}