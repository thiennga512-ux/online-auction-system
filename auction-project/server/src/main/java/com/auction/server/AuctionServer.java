package com.auction.server;

import com.auction.model.*;
import com.auction.service.Auction;
import com.auction.strategy.*;
import java.io.*;
import java.net.*;

/**
 * AuctionServer: Lớp chạy chính của phía Server.
 * Quản lý kết nối Socket và khởi tạo hệ thống.
 */
public class AuctionServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("=== HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - SERVER IS STARTING ===");

        // 1. Khởi tạo Database
        DatabaseManager.initializeDatabase();

        // 2. Chạy Demo kiểm tra logic (Có thể comment lại sau)
        runDemo();

        // 3. Khởi động Socket Server
        startSocketServer();
    }

    private static void startSocketServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("📍 Server đang lắng nghe tại cổng " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔌 Có kết nối mới từ: " + clientSocket.getInetAddress());

                // Tạm thời chỉ in ra thông báo, sau này sẽ tạo Thread để xử lý từng Client
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi Server Socket: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("Chào mừng bạn đến với Hệ thống Đấu giá!");
            String input;
            while ((input = in.readLine()) != null) {
                System.out.println("Nhận từ client: " + input);
                out.println("Server đã nhận: " + input);
            }
        } catch (IOException e) {
            System.err.println("Lỗi xử lý client: " + e.getMessage());
        }
    }

    private static void runDemo() {
        System.out.println("\n--- RUNNING SYSTEM LOGIC DEMO ---");

        // Tạo dữ liệu mẫu
        Seller seller = new Seller("S1", "Nguyễn Văn Bán", "seller01", "seller@example.com", "123", "09123", "Nam",
                "1990-01-01", null, true, 5.0);
        Bidder bidder1 = new Bidder("B1", "Trần Văn Mua", "bidder01", "bidder1@example.com", "123", "09124", "Nam",
                "1995-01-01", null, true, 1000000);
        Bidder bidder2 = new Bidder("B2", "Lê Thị Đấu", "bidder02", "bidder2@example.com", "123", "09125", "Nữ",
                "1998-05-05", null, true, 2000000);

        // Tạo sản phẩm và phiên đấu giá (Thời gian: 10 phút)
        ItemData artData = new ItemData("A1", "Bức tranh Mùa Thu", "Tranh sơn dầu cổ điển", 500000);
        artData.artistName = "Họa sĩ Thiên Nga";
        Item artItem = seller.createItem(new ArtFactory(), artData);
        Auction auction = new Auction("AUC001", artItem, 10);

        // Đăng ký nhận thông báo
        auction.registerObserver(bidder1);
        auction.registerObserver(bidder2);

        // Thiết lập chiến lược
        bidder1.setStrategy(new NormalBiddingStrategy());
        bidder2.setStrategy(new AutoBiddingStrategy(1500000, 100000));

        // Đặt giá đầu tiên
        System.out.println(">>> Bidder 1 đặt giá 600,000");
        bidder1.getStrategy().placeBid(bidder1, auction, 600000);

        // Vì Bidder 2 có AutoBiddingStrategy, nó sẽ tự động phản ứng khi nhận được
        // notify từ Auction
        // (Điều này xảy ra bên trong bidder1.getStrategy().placeBid ->
        // auction.placeNewBid -> notifyObservers -> bidder2.update)

        auction.closeAuction();
        System.out.println("--- END DEMO ---\n");
    }
}
