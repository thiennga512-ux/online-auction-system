package com.auction.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import com.auction.enums.ItemCategory;

import static org.junit.jupiter.api.Assertions.*;

public class ItemTest {

    private Item item;

    @BeforeEach
    public void setUp() {
        item = new Item("Laptop Gaming", "High end gaming laptop", 1500.0, 50.0, "image.png", "seller-123", ItemCategory.ELECTRONICS);
    }

    @Test
    public void testConstructor_NewItem() {
        assertNotNull(item.getId(), "BaseEntity should generate an ID");
        assertNotNull(item.getcreatedAt(), "BaseEntity should set createdAt");
        assertNotNull(item.getcreatedAt(), "BaseEntity should set updatedAt");

        assertEquals("Laptop Gaming", item.getName());
        assertEquals("High end gaming laptop", item.getDescription());
        assertEquals(1500.0, item.getStartingPrice());
        assertEquals(50.0, item.getBidIncrement());
        assertEquals("image.png", item.getImageUrl());
        assertEquals("seller-123", item.getSellerId());
        assertEquals(ItemCategory.ELECTRONICS, item.getCategory());
    }

    @Test
    public void testConstructor_FromDatabase() {
        LocalDateTime now = LocalDateTime.now();
        Item dbItem = new Item("item-456", now, now, "Vintage Art", "Old painting", 
                               2000.0, 100.0, "art.png", "seller-456", ItemCategory.ART);

        assertEquals("item-456", dbItem.getId());
        assertEquals(now, dbItem.getcreatedAt());
        assertEquals(now, dbItem.getcreatedAt());
        assertEquals("Vintage Art", dbItem.getName());
        assertEquals(ItemCategory.ART, dbItem.getCategory());
    }

    @Test
    public void testSettersAndGetters() {
        item.setName("Updated Laptop");
        assertEquals("Updated Laptop", item.getName());

        item.setStartingPrice(1600.0);
        assertEquals(1600.0, item.getStartingPrice());

        item.setCategory(ItemCategory.VEHICLE);
        assertEquals(ItemCategory.VEHICLE, item.getCategory());
    }
}
