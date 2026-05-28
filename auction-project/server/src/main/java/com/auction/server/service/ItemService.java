package com.auction.server.service;

import com.auction.model.Item;
import com.auction.server.dao.ItemDAO;
import com.auction.exception.DatabaseException;
import java.sql.SQLException;
import java.util.List;
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
            throw DatabaseException.queryFailed("tìm sản phẩm", e);
        }
    }

    public void markItemAsSold(String itemId) {
        try {
            itemDAO.updateStatus(itemId, false);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("đánh dấu sản phẩm đã bán", e);
        }
    }
    public void markItemAsAvailable(String itemId) {
        try {
            itemDAO.updateStatus(itemId, true);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("đánh dấu sản phẩm còn hàng", e);
        }
    }

    public List<Item> getItemsBySeller(String sellerId) {
        try {
            return itemDAO.getItemsBySeller(sellerId);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("lấy danh sách sản phẩm", e);
        }
    }

    public void listItem(Item item) {
        try {
            itemDAO.save(item);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("đăng sản phẩm", e);
        }
    }
}

