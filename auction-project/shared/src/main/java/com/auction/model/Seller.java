package com.auction.model;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.auction.enums.UserRole;
import com.auction.service.auction.AuctionSession;


public class Seller extends Bidder {
    // 2. Thuộc tính danh sách quản lý bán hàng
    private List<AuctionSession> registrationHistory; 
    private List<AuctionSession> soldHistory;         
    private String shopName;
    private String citizenId;
    
    // Constructor cho đăng ký mới
    public Seller(String username, String passwordHash, String email, String fullName, String gender, String dateOfBirth, String shopName, String citizenId) {
        super(username, passwordHash, email, fullName, UserRole.SELLER, gender, dateOfBirth);
        this.registrationHistory = new ArrayList<>();
        this.soldHistory = new ArrayList<>();
        this.shopName="";
        this.citizenId="";
    }

    // Constructor nạp dữ liệu từ MySQL
    public Seller(String id, LocalDateTime createdAt, LocalDateTime updateAt, String username, 
                  String passwordHash, String email, String fullName, boolean active, String gender, String dateOfBirth, double balance, double frozenBalance, String shippingAddress, String shopName, String citizenId) {
        super(id, createdAt, updateAt, username, passwordHash, email, fullName, UserRole.SELLER, active, gender, dateOfBirth, balance, frozenBalance, shippingAddress);
        this.registrationHistory = new ArrayList<>();
        this.soldHistory = new ArrayList<>();
        this.shopName=shopName;
        this.citizenId=citizenId;
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    @Override
    public String getDashboardView() {
        return "/views/seller_dashboard.fxml";
    }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }

    public List<AuctionSession> getRegistrationHistory() { return registrationHistory; }
    public List<AuctionSession> getSoldHistory() { return soldHistory; }

}