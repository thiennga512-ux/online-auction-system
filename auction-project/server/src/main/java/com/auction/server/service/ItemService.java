package com.auction.server.service;

import com.auction.model.Item;
import com.auction.server.dao.ItemDAO;
import java.sql.SQLException;
import java.util.Optional;

public class ItemService {
    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    public Optional<Item> findById(String itemId) {
        try {
            return itemDAO.findById(itemId);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm sản phẩm: " + e.getMessage(), e);
        }
    }

    public void markItemAsSold(String itemId) {
        try {
            itemDAO.updateStatus(itemId, false);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi đánh dấu sản phẩm đã bán: " + e.getMessage(), e);
        }
    }
}
