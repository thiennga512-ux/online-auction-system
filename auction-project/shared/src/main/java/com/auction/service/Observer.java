package com.auction.service;

// ----------------------------------------
// Design Pattern: OBSERVER (Người quan sát)
// ----------------------------------------

// Interface đóng vai trò là "Người nghe"
public interface Observer {
    void update(String message);
}