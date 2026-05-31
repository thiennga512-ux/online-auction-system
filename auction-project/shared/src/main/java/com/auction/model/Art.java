package com.auction.model;

import java.time.LocalDateTime;
import com.auction.enums.ItemCategory;

public class Art extends Item {
    private String artistName;    // Tên họa sĩ/tác giả
    private int yearCreated;      // Năm sáng tác
    private String medium;        // Chất liệu (ví dụ: Sơn dầu, Màu nước, Điêu khắc gỗ)
    private boolean authenticated;
    private String certificateId;
    private String dimensions;

    // Constructor cho tạo mới tác phẩm: Gán cứng Category.ART
    public Art(String name, String description, double startingPrice, double bidIncrement, 
               String imageUrl, String sellerId, 
               String artistName, int yearCreated, String medium,
               boolean authenticated, String certificateId, String dimensions) {
        
        super(name, description, startingPrice, bidIncrement, imageUrl, sellerId, ItemCategory.ART);
        this.artistName = artistName;
        this.yearCreated = yearCreated;
        this.medium = medium;
        this.authenticated = authenticated;
        this.certificateId = certificateId;
        this.dimensions = dimensions;
    }

    // Constructor nạp dữ liệu từ MySQL (dùng khi lấy dữ liệu cũ lên)
    public Art(String id, LocalDateTime createdAt, LocalDateTime updateAt, 
               String name, String description, double startingPrice, double bidIncrement, 
               String imageUrl, String sellerId, 
               String artistName, int yearCreated, String medium,
               boolean authenticated, String certificateId, String dimensions) {
        
        super(id, createdAt, updateAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId, ItemCategory.ART);
        this.artistName = artistName;
        this.yearCreated = yearCreated;
        this.medium = medium;
        this.authenticated = authenticated;
        this.certificateId = certificateId;
        this.dimensions = dimensions;
    }

    // Override lại để đảm bảo luôn trả về đúng danh mục nghệ thuật
    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ART;
    }

    @Override
    public void printInfo() {
        System.out.println("---------- TÁC PHẨM NGHỆ THUẬT ----------");
        super.printInfo(); // In các thông tin cơ bản: Tên, giá khởi điểm, Seller ID
        System.out.printf("Tác giả: %s | Năm sáng tác: %d | Chất liệu: %s | Xác thực: %s | Kích thước: %s%n",
                artistName, yearCreated, medium, authenticated ? "Có" : "Không", dimensions);
        System.out.println("------------------------------------------");
    }

    // --- Getters & Setters ---
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public int getYearCreated() { return yearCreated; }
    public void setYearCreated(int yearCreated) { this.yearCreated = yearCreated; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }

    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

    public String getCertificateId() { return certificateId; }
    public void setCertificateId(String certificateId) { this.certificateId = certificateId; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
}