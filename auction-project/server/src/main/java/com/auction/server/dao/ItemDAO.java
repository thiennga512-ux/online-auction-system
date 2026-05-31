package com.auction.server.dao;

import com.auction.factory.ItemFactory;
import com.auction.model.*;
import com.auction.enums.ItemCategory;
import com.auction.server.database.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ItemDAO {

    private static final Map<String, Item> itemCache = new java.util.concurrent.ConcurrentHashMap<>();

    private Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    // --- CREATE ---
    public void save(Item item) throws SQLException {
        String sql = """
                INSERT INTO items
                (id, name, description, base_price, min_increment, seller_id, category, image_url, available, listed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getStartingPrice());
            ps.setDouble(5, item.getBidIncrement());
            ps.setString(6, item.getSellerId());
            ps.setString(7, item.getCategory().name());
            ps.setString(8, item.getImageUrl());
            ps.setInt(9, item.isAvailable() ? 1 : 0);
            ps.setString(10, item.getcreatedAt().toString());
            ps.executeUpdate();
        }

        switch (item.getCategory()) {
            case ELECTRONICS -> saveElectronicsDetails((Electronics) item);
            case ART -> saveArtDetails((Art) item);
            case VEHICLE -> saveVehicleDetails((Vehicle) item);
            default -> throw new IllegalArgumentException("Unexpected value: " + item.getCategory());
        }
        
        // Cache sau khi lưu thành công
        itemCache.put(item.getId(), item);
    }

    private void saveElectronicsDetails(Electronics e) throws SQLException {
        String sql = "INSERT INTO electronics_details (item_id, brand, model, warranty_months, condition_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getId());
            ps.setString(2, e.getBrand());
            ps.setString(3, e.getModel());
            ps.setInt(4, e.getWarrantyMonths());
            ps.setString(5, e.getCondition().name());
            ps.executeUpdate();
        }
    }

    private void saveArtDetails(Art a) throws SQLException {
        String sql = "INSERT INTO art_details (item_id, artist_name, creation_year, medium, authenticated, certificate_id, dimensions) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getId());
            ps.setString(2, a.getArtistName());
            ps.setInt(3, a.getYearCreated());
            ps.setString(4, a.getMedium());
            ps.setInt(5, a.isAuthenticated() ? 1 : 0);
            ps.setString(6, a.getCertificateId());
            ps.setString(7, a.getDimensions());
            ps.executeUpdate();
        }
    }

    private void saveVehicleDetails(Vehicle v) throws SQLException {
        String sql = "INSERT INTO vehicle_details (item_id, vehicle_type, make, model, year, mileage, fuel_type, transmission, color, license_plate, has_valid_registry) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getId());
            ps.setString(2, v.getVehicleType());
            ps.setString(3, v.getMake());
            ps.setString(4, v.getModel());
            ps.setInt(5, v.getYear());
            ps.setInt(6, v.getMileage());
            ps.setString(7, v.getFuelType().name());
            ps.setString(8, v.getTransmission());
            ps.setString(9, v.getColor());
            ps.setString(10, v.getLicensePlate());
            ps.setInt(11, v.isHasValidRegistry() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    // --- READ ---
    public Optional<Item> findById(String id) throws SQLException {
        if (itemCache.containsKey(id)) {
            return Optional.of(itemCache.get(id));
        }
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Item item = mapRowToItem(rs);
                    itemCache.put(id, item);
                    return Optional.of(item);
                }
            }
        }
        return Optional.empty();
    }

    private Item mapRowToItem(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        double basePrice = rs.getDouble("base_price");
        double minInc = rs.getDouble("min_increment");
        String sellerId = rs.getString("seller_id");
        ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
        String imgUrl = rs.getString("image_url");
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("listed_at"));

        // Giả sử updatedAt lấy từ DB hoặc dùng tạm createdAt nếu chưa có cột riêng
        LocalDateTime updatedAt = createdAt;

        Map<String, Object> extraData = new HashMap<>();

        // Truy vấn bảng detail dựa trên category
        String detailSql = switch (category) {
            case ELECTRONICS -> "SELECT * FROM electronics_details WHERE item_id = ?";
            case ART -> "SELECT * FROM art_details WHERE item_id = ?";
            case VEHICLE -> "SELECT * FROM vehicle_details WHERE item_id = ?";
            default -> throw new IllegalArgumentException("Unexpected value: " + category);
        };

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(detailSql)) {
            ps.setString(1, id);
            try (ResultSet rsDetail = ps.executeQuery()) {
                if (rsDetail.next()) {
                    if (category == ItemCategory.ELECTRONICS) {
                        extraData.put("brand", rsDetail.getString("brand"));
                        extraData.put("model", rsDetail.getString("model"));
                        extraData.put("warrantyMonths", rsDetail.getInt("warranty_months"));
                        extraData.put("condition", Electronics.Condition.valueOf(rsDetail.getString("condition_type")));
                    } else if (category == ItemCategory.ART) {
                        extraData.put("artistName", rsDetail.getString("artist_name"));
                        extraData.put("yearCreated", rsDetail.getInt("creation_year"));
                        extraData.put("medium", rsDetail.getString("medium"));
                        extraData.put("authenticated", rsDetail.getInt("authenticated") == 1);
                        extraData.put("certificateId", rsDetail.getString("certificate_id"));
                        extraData.put("dimensions", rsDetail.getString("dimensions"));
                    } else if (category == ItemCategory.VEHICLE) {
                        extraData.put("vehicleType", rsDetail.getString("vehicle_type"));
                        extraData.put("make", rsDetail.getString("make"));
                        extraData.put("model", rsDetail.getString("model"));
                        extraData.put("year", rsDetail.getInt("year"));
                        extraData.put("mileage", rsDetail.getInt("mileage"));
                        extraData.put("fuelType", Vehicle.FuelType.valueOf(rsDetail.getString("fuel_type")));
                        extraData.put("transmission", rsDetail.getString("transmission"));
                        extraData.put("color", rsDetail.getString("color"));
                        extraData.put("licensePlate", rsDetail.getString("license_plate"));
                        extraData.put("hasValidRegistry", rsDetail.getInt("has_valid_registry") == 1);
                    }
                }
            }
        }

        // GỌI FACTORY ĐỂ TẠO OBJECT
        Item item = ItemFactory.reconstructFromDb(category, id, createdAt, updatedAt, name, description, basePrice, minInc,
                imgUrl, sellerId, extraData);
        item.setAvailable(rs.getInt("available") == 1);
        return item;
    }

    // --- UPDATE ---
    public void updateStatus(String itemId, boolean available) throws SQLException {
        String sql = "UPDATE items SET available = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setString(2, itemId);
            ps.executeUpdate();
        }
        // Cập nhật trạng thái hiển thị trong cache
        Item cached = itemCache.get(itemId);
        if (cached != null) {
            cached.setAvailable(available);
        }
    }

    public List<Item> getItemsBySeller(String sellerId) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item item = mapRowToItem(rs);
                    itemCache.put(item.getId(), item);
                    items.add(item);
                }
            }
        }
        return items;
    }
}