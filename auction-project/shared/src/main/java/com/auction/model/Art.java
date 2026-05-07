package com.auction.model;
<<<<<<< HEAD
import java.time.LocalDateTime;
import model.enums.ItemCategory;

=======

/**
 * Lớp Art: Đại diện cho các tác phẩm nghệ thuật trong đấu giá.
 */
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
public class Art extends Item {
    private String artistName;    // Tên họa sĩ/tác giả
    private int yearCreated;      // Năm sáng tác
    private String medium;        // Chất liệu (ví dụ: Sơn dầu, Màu nước, Điêu khắc gỗ)

<<<<<<< HEAD
    // Constructor cho tạo mới tác phẩm: Gán cứng Category.ART
    public Art(String name, String description, double startingPrice, double bidIncrement, 
               String imageUrl, String sellerId, 
               String artistName, int yearCreated, String medium) {
        
        // Gọi super và truyền thẳng Category.ART từ Enum Category
        super(name, description, startingPrice, bidIncrement, imageUrl, sellerId, ItemCategory.ART);
        this.artistName = artistName;
        this.yearCreated = yearCreated;
        this.medium = medium;
    }

    // Constructor nạp dữ liệu từ MySQL (dùng khi lấy dữ liệu cũ lên)
    public Art(String id, LocalDateTime createdAt, LocalDateTime updateAt, 
               String name, String description, double startingPrice, double bidIncrement, 
               String imageUrl, String sellerId, 
               String artistName, int yearCreated, String medium) {
        
        super(id, createdAt, updateAt, name, description, startingPrice, bidIncrement, imageUrl, sellerId, ItemCategory.ART);
        this.artistName = artistName;
        this.yearCreated = yearCreated;
        this.medium = medium;
    }

    // Override lại để đảm bảo luôn trả về đúng danh mục nghệ thuật
    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ART;
=======
    private String artistName; // Tên họa sĩ
    private int creationYear; // Năm sáng tác
    private String medium; // Chất liệu (Sơn dầu, acrylic, ...)
    private boolean authenticated; // Đã kiểm định chưa
    private String certificateId; // Mã số chứng chỉ kiểm định
    private String dimensions; // Kích thước

    public Art(String id, String name, String description, double startingPrice, String artistName, int creationYear) {
        super(id, name, description, startingPrice);
        this.artistName = artistName;
        this.creationYear = creationYear;
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
    }

    @Override
    public void printInfo() {
<<<<<<< HEAD
        System.out.println("---------- TÁC PHẨM NGHỆ THUẬT ----------");
        super.printInfo(); // In các thông tin cơ bản: Tên, giá khởi điểm, Seller ID
        System.out.printf("Tác giả: %s | Năm sáng tác: %d%n", artistName, yearCreated);
        System.out.printf("Chất liệu: %s%n", medium);
        System.out.println("------------------------------------------");
    }

    // --- Getters & Setters ---
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public int getYearCreated() { return yearCreated; }
    public void setYearCreated(int yearCreated) { this.yearCreated = yearCreated; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }
=======
        System.out.println("🎨 Tác phẩm nghệ thuật: " + name);
        System.out.println("   - Họa sĩ: " + artistName);
        System.out.println("   - Năm sáng tác: " + (creationYear > 0 ? creationYear : "Không rõ"));
        System.out.println("   - Giá khởi điểm: " + startingPrice + " VNĐ");
        if (medium != null) System.out.println("   - Chất liệu: " + medium);
        if (dimensions != null) System.out.println("   - Kích thước: " + dimensions);
        System.out.println("   - Trạng thái kiểm định: " + (authenticated ? "Đã kiểm định (" + certificateId + ")" : "Chưa kiểm định"));
    }

    // Getter & Setter
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public int getCreationYear() { return creationYear; }
    public void setCreationYear(int creationYear) { this.creationYear = creationYear; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }

    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

    public String getCertificateId() { return certificateId; }
    public void setCertificateId(String certificateId) { this.certificateId = certificateId; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
>>>>>>> e819ca10d6124354447960e56f54514b86f497ff
}