package com.auction.model;

public class ArtFactory implements ItemFactory {

    @Override
    public Item createItem(ItemData data) {

        if (data.artistName == null) {
            throw new IllegalArgumentException("Art cần artistName");
        }

        return new Art(
                data.id,
                data.name,
                data.description,
                data.price,
                data.artistName
        );
    }
}