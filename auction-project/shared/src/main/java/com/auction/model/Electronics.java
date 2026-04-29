package com.auction.model;

public class Electronics extends Item {

    private int warrantyPeriod; // thời gian bảo hành (tháng)

    public Electronics(String id, String name, String desc, double price, int warranty) {
        super(id, name, desc, price);
        this.warrantyPeriod = warranty;
    }

    @Override
    public void printInfo() {
        System.out.println("Electronics: " + name
                + " | Price: " + startingPrice
                + " | Warranty: " + warrantyPeriod + " months");
    }
}