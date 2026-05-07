package com.auction.model;

/**
 * ArtFactory: Chuyên tạo các đối tượng Art.
 */
public class ArtFactory implements ItemFactory {

    @Override
    public Item createItem(ItemData data) {
        if (data.artistName == null) {
            throw new IllegalArgumentException("Art cần artistName");
        }

        // Mặc định creationYear là 0 nếu không có trong data (có thể mở rộng ItemData sau)
        return new Art(
                data.id,
                data.name,
                data.description,
                data.price,
                data.artistName,
                0 // creationYear mặc định
        );
    }
}