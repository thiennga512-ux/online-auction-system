package com.auction.model;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.auction.enums.UserRole;
import com.auction.service.auction.AuctionSession;


public class Seller extends User {
    // 1. Thuộc tính tài chính
    private double balance; 

    // 2. Thuộc tính danh sách quản lý bán hàng
    private List<AuctionSession> registrationHistory; 
    private List<AuctionSession> soldHistory;         
    private double rating;
    private int ratingCount;
    private String shopName;
    private String citizenId;
    
    // Constructor cho đăng ký mới
    public Seller(String username, String passwordHash, String email, String fullName,double rating) {
        super(username, passwordHash, email, fullName, UserRole.SELLER);
        this.balance = 0.0;
        this.registrationHistory = new ArrayList<>();
        this.soldHistory = new ArrayList<>();
        this.rating=0.0;
        this.ratingCount=0;
    }

    // Constructor nạp dữ liệu từ MySQL
    public Seller(String id, LocalDateTime createdAt, LocalDateTime updateAt, String username, 
                  String passwordHash, String email, String fullName, boolean active, double balance,double rating, int ratingCount) {
        super(id, createdAt, updateAt, username, passwordHash, email, fullName, UserRole.SELLER, active);
        this.balance = balance;
        this.registrationHistory = new ArrayList<>();
        this.soldHistory = new ArrayList<>();
        this.rating=rating;
        this.ratingCount=ratingCount;
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    @Override
    public String getDashboardView() {
        return "/views/seller_dashboard.fxml";
    }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public double getRating(){
        return rating;
    }
    public void setRating(double rating){
        this.rating=rating;
    }
    public int getRatingCount(){
        return ratingCount;
    }
    public void setRatingCount(int ratingCount){
        this.ratingCount=ratingCount;
    }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }

    public List<AuctionSession> getRegistrationHistory() { return registrationHistory; }
    public List<AuctionSession> getSoldHistory() { return soldHistory; }

}