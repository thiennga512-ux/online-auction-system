package com.auction.model;
<<<<<<< HEAD
=======

>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.enums.UserRole;
import model.auction.AuctionSession;
import utils.Validator;

<<<<<<< HEAD
public class Seller extends User {
    // 1. Thuộc tính tài chính
    private double balance; // Doanh thu thực nhận sau khi trừ hoa hồng

    // 2. Thuộc tính danh sách quản lý bán hàng
    private List<AuctionSession> registrationHistory; 
    private List<AuctionSession> soldHistory;         
    private double rating;
    
    // Constructor cho đăng ký mới
    public Seller(String username, String passwordHash, String email, String fullName,double rating) {
        super(username, passwordHash, email, fullName, UserRole.SELLER);
        this.balance = 0.0;
        this.registrationHistory = new ArrayList<>();
        this.soldHistory = new ArrayList<>();
        this.rating=0.0;
    }

    // Constructor nạp dữ liệu từ MySQL
    public Seller(String id, LocalDateTime createdAt, LocalDateTime updateAt, String username, 
                  String passwordHash, String email, String fullName, boolean active, double balance,double rating) {
        super(id, createdAt, updateAt, username, passwordHash, email, fullName, UserRole.SELLER, active);
        this.balance = balance;
        this.registrationHistory = new ArrayList<>();
        this.soldHistory = new ArrayList<>();
        this.rating=rating;
=======
/**
 * Seller: Người bán hàng.
 */
public class Seller extends User {

    private double rating; // Điểm uy tín
    private List<Item> items; // Danh sách sản phẩm đã tạo

    public Seller(String id, String fullName, String username, String email, String password, String phoneNumber, String gender, String dateOfBirth, LocalDateTime createdAt, boolean active, double rating) {
        super(id, fullName, username, email, password, phoneNumber, gender, dateOfBirth, createdAt, active);
        this.rating = rating;
        this.items = new ArrayList<>();
    }

    public Seller(String fullName, String username, String email, String password, String gender, String dateOfBirth, double rating) {
        super(fullName, username, email, password, gender, dateOfBirth);
        this.rating = rating;
        this.items = new ArrayList<>();
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    @Override
    public String getDashboardView() {
<<<<<<< HEAD
        return "/views/seller_dashboard.fxml";
    }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public double getRating(){
        return rating;
    }
    public void setRating(double rating){
        Validator.validateRating(rating);
        this.rating=rating;
=======
        return "--- GIAO DIỆN NGƯỜI BÁN ---";
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    // Tạo sản phẩm bằng Factory Pattern
    public Item createItem(ItemFactory factory, ItemData data) {
        Item item = factory.createItem(data);
        items.add(item);
        System.out.println(getUsername() + " đã tạo sản phẩm: " + item.getName());
        return item;
    }

    public List<Item> getItems() {
        return items;
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
    }
    public List<AuctionSession> getRegistrationHistory() { return registrationHistory; }
    public List<AuctionSession> getSoldHistory() { return soldHistory; }
}