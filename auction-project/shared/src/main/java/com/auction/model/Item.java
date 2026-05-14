package com.auction.model;

import java.time.LocalDateTime;
import com.auction.enums.ItemCategory; 

public class Item extends BaseEntity {
    private String name;
    private String description;
    private double startingPrice; // Giá khởi điểm
    private double bidIncrement;  // Bước giá tối thiểu
    private String imageUrl;      // Đường dẫn ảnh sản phẩm
    private String sellerId;      // ID của người đăng bán (Seller)
    private ItemCategory category;    // Loại sản phẩm từ Enum Category

    // Constructor cho tạo mới sản phẩm (BaseEntity tự sinh UUID)
    public Item(String name, String description, double startingPrice, 
                double bidIncrement, String imageUrl, String sellerId, ItemCategory category) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.imageUrl = imageUrl;
        this.sellerId = sellerId;
        this.category = category;
    }

    // Constructor dùng để nạp dữ liệu từ MySQL
    public Item(String id, LocalDateTime createdAt, LocalDateTime updateAt, 
                String name, String description, double startingPrice, 
                double bidIncrement, String imageUrl, String sellerId, ItemCategory category) {
        super(id, createdAt, updateAt);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.imageUrl = imageUrl;
        this.sellerId = sellerId;
        this.category = category;
    }

    @Override
    public void printInfo() {
        System.out.printf("[Sản phẩm] ID=%s | Tên: %s | Danh mục: %s | Giá khởi điểm: %.2f | Người bán ID: %s%n",
                getId(), name, category, startingPrice, sellerId);
    }

    // --- Getters & Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getBidIncrement() { return bidIncrement; }
    public void setBidIncrement(double bidIncrement) { this.bidIncrement = bidIncrement; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public boolean isBidValid(double currentPrice, double proposedBid) {
    return proposedBid >= currentPrice + bidIncrement;
  }

    public boolean isAvailable() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isAvailable'");
    }

}