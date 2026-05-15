package com.auction.server.dao;

import com.auction.factory.ItemFactory;
import com.auction.model.*;
import com.auction.enums.ItemCategory;
import com.auction.server.database.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ItemDAO {

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
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getStartingPrice());
            ps.setDouble(5, item.getBidIncrement());
            ps.setString(6, item.getSellerId());
            ps.setString(7, item.getCategory().name());
            ps.setString(8, item.getImageUrl());
            ps.setInt(9, item.isvailable() ? 1 : 0);
            ps.setString(10, item.getcreatedAt().toString());
            ps.executeUpdate();
        }

        switch (item.getCategory()) {
            case ELECTRONICS -> saveElectronicsDetails((Electronics) item);
            case ART -> saveArtDetails((Art) item);
            case VEHICLE -> saveVehicleDetails((Vehicle) item);
            default -> throw new IllegalArgumentException("Unexpected value: " + item.getCategory());
        }
    }

    private void saveElectronicsDetails(Electronics e) throws SQLException {
        String sql = "INSERT INTO electronics_details (item_id, brand, model, warranty_months, condition_type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, e.getId());
            ps.setString(2, e.getBrand());
            ps.setString(3, e.getModel());
            ps.setInt(4, e.getWarrantyMonths());
            ps.setString(5, e.getCondition().name());
            ps.executeUpdate();
        }
    }

    private void saveArtDetails(Art a) throws SQLException {
        String sql = "INSERT INTO art_details (item_id, artist_name, creation_year, medium) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, a.getId());
            ps.setString(2, a.getArtistName());
            ps.setInt(3, a.getcreatedAt().getYear());
            ps.setString(4, a.getMedium());
            ps.executeUpdate();
        }
    }

    private void saveVehicleDetails(Vehicle v) throws SQLException {
        String sql = "INSERT INTO vehicle_details (item_id, make, model, year, mileage, fuel_type) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, v.getId());
            ps.setString(2, v.getMake());
            ps.setString(3, v.getModel());
            ps.setInt(4, v.getYear());
            ps.setInt(5, v.getMileage());
            ps.setString(6, v.getFuelType().name());
            ps.executeUpdate();
        }
    }

    // --- READ ---
    public Optional<Item> findById(String id) throws SQLException {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(mapRowToItem(rs));
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

        try (PreparedStatement ps = getConnection().prepareStatement(detailSql)) {
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
                    } else if (category == ItemCategory.VEHICLE) {
                        extraData.put("make", rsDetail.getString("make"));
                        extraData.put("model", rsDetail.getString("model"));
                        extraData.put("year", rsDetail.getInt("year"));
                        extraData.put("mileage", rsDetail.getInt("mileage"));
                        extraData.put("fuelType", Vehicle.FuelType.valueOf(rsDetail.getString("fuel_type")));
                    }
                }
            }
        }

        // GỌI FACTORY ĐỂ TẠO OBJECT
        return ItemFactory.reconstructFromDb(category, id, createdAt, updatedAt, name, description, basePrice, minInc,
                imgUrl, sellerId, extraData);
    }

    // --- UPDATE ---
    public void updateStatus(String itemId, boolean available) throws SQLException {
        String sql = "UPDATE items SET available = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setString(2, itemId);
            ps.executeUpdate();
        }
    }
}