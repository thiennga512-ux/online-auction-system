package com.auction.service;

// Interface đóng vai trò là "Người phát thanh"
public interface Subject {
    void registerObserver(Observer observer); // Cho phép người khác đăng ký theo dõi
    void removeObserver(Observer observer);   // Hủy theo dõi
    void notifyObservers(String message);     // Gửi tin nhắn cho toàn bộ người đang theo dõi
}