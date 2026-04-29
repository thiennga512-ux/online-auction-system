package com.auction.service;

import com.auction.model.BidTransaction;
import com.auction.model.Bidder;
import com.auction.model.Item;

import java.util.ArrayList;
import java.util.List;

// Phiên đấu giá: Nơi mọi thứ kết nối với nhau.
// Nó "implements Subject" để có thể thông báo giá mới cho các Bidder đang xem.
public class Auction implements Subject {
    private Item item;
    private double highestBid;
    private Bidder winner;
    private boolean isOngoing; // Trạng thái: Đang diễn ra hay đã đóng?
    
    private List<BidTransaction> historyList; // Lịch sử đặt giá
    private List<Observer> observersList;     // Danh sách người theo dõi

    public Auction(Item item) {
        this.item = item;
        this.highestBid = item.getStartingPrice(); // Giá cao nhất lúc đầu chính là giá khởi điểm
        this.isOngoing = true;
        this.historyList = new ArrayList<>();
        this.observersList = new ArrayList<>();
    }

    // --- CÁC HÀM CỦA SUBJECT (OBSERVER PATTERN) ---
    @Override
    public void registerObserver(Observer observer) {
        observersList.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observersList.remove(observer);
    }

    @Override
    public void notifyObservers(String message) {
        // Duyệt qua tất cả người đang theo dõi và gọi hàm update của họ
        for (Observer obs : observersList) {
            obs.update(message);
        }
    }

    // --- LOGIC ĐẤU GIÁ ---
    public void placeNewBid(Bidder bidder, double amount) {
        if (!isOngoing) {
            System.out.println("❌ LỖI: Phiên đấu giá đã kết thúc!");
            return;
        }

        if (amount > highestBid && amount <= bidder.getBalance()) {
            // Cập nhật giá và người dẫn đầu
            highestBid = amount;
            winner = bidder;
            
            // Lưu vào lịch sử
            BidTransaction transaction = new BidTransaction(bidder, amount);
            historyList.add(transaction);

            // BÁO CHO TẤT CẢ MỌI NGƯỜI CÓ GIÁ MỚI (Real-time update)
            notifyObservers("🔥 Báo động: " + bidder.getUsername() + " vừa đưa ra giá cao mới là " + amount + " VNĐ cho " + item.getName());
        } else {
            System.out.println("❌ LỖI: Giá không hợp lệ hoặc " + bidder.getUsername() + " không đủ tiền cược!");
        }
    }

    public void closeAuction() {
        isOngoing = false;
        if (winner != null) {
            notifyObservers("🏆 PHIÊN ĐẤU GIÁ KẾT THÚC! Chúc mừng " + winner.getUsername() + " đã mua được " + item.getName() + " với giá " + highestBid);
        } else {
            notifyObservers("⚠️ Phiên đấu giá kết thúc! Không có ai đặt giá.");
        }
    }
}