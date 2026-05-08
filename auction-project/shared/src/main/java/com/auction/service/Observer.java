package com.auction.service;

import com.auction.service.auction.Auction;

// ----------------------------------------

// Design Pattern: OBSERVER (Người quan sát)
// ----------------------------------------

// Interface đóng vai trò là "Người nghe"
public interface Observer {
    void update(String message, Auction auction);
}