package com.auction.model;

/**
 * Lớp Art: Đại diện cho các tác phẩm nghệ thuật trong đấu giá.
 */
public class Art extends Item {

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
    }

    @Override
    public void printInfo() {
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
}