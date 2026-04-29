package com.auction.model;

public class Art extends Item {

    private String artistName; // tên họa sĩ

    public Art(String id, String name, String desc, double price, String artist) {
        super(id, name, desc, price);
        this.artistName = artist;
    }

    @Override
    public void printInfo() {
        System.out.println("Art: " + name
                + " | Artist: " + artistName
                + " | Price: " + startingPrice);
    }
}