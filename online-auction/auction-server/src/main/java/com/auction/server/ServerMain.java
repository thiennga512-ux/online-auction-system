package com.auction.server;

import com.auction.common.factory.ItemFactory;
import com.auction.common.model.auction.AuctionSession;
import com.auction.common.model.item.Electronics;
import com.auction.common.model.user.Admin;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.database.DatabaseManager;
import com.auction.server.network.RequestDispatcher;
import com.auction.server.network.SocketServer;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.server.service.ItemService;
import com.auction.server.service.RegistrationService;
import com.auction.server.service.UserService;
import com.auction.server.service.AutoBidService;
import com.auction.server.scheduler.AuctionTimerManager;
import java.time.LocalDateTime;

/**
 * ============================================================
 * ServerMain — Điểm khởi chạy của Server (Bản hoàn thiện)
 * ============================================================
 */
public class ServerMain {

  public static void main(String[] args) {
    System.out.println("🌟 Đang khởi động Hệ thống Đấu giá Trực tuyến...");

    try {
      DatabaseManager db = DatabaseManager.getInstance();

      // 1. Khởi tạo DAOs
      UserDAO userDAO = new UserDAO();
      ItemDAO itemDAO = new ItemDAO();
      BidDAO bidDAO = new BidDAO();
      AuctionSessionDAO auctionSessionDAO = new AuctionSessionDAO(itemDAO, bidDAO);

      // 2. Khởi tạo Services
      ItemService itemService = new ItemService(itemDAO);
      UserService userService = new UserService(userDAO);
      AuthService authService = new AuthService(userDAO);
      RegistrationService registrationService = new RegistrationService(userDAO);
      AuctionService auctionService = new AuctionService(auctionSessionDAO, bidDAO, userDAO, itemService);
      AutoBidService autoBidService = new AutoBidService(auctionService, userService);
      com.auction.server.service.NotificationService.getInstance().init(userDAO);

      // 3. Khởi tạo Timer
      AuctionTimerManager timerManager = AuctionTimerManager.getInstance();
      timerManager.initialize(auctionService);
      timerManager.start();

      // 4. Shutdown Hook
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\n🛑 Đang tắt Server an toàn...");
        timerManager.stop();
        db.close();
      }));

      // 5. Seed Data (Dữ liệu mẫu)
      if (userDAO.count() == 0) {
        System.out.println("📦 Database trống. Đang khởi tạo dữ liệu mẫu...");
        initializeDemoData(registrationService, userService, itemService, auctionService);
      } else {
        System.out.println("📂 Database đã có dữ liệu. Bỏ qua bước tạo mẫu.");
      }

      // 6. Khởi động Socket Server
      RequestDispatcher dispatcher = new RequestDispatcher(
          userService, authService, registrationService, auctionService, itemService, autoBidService);
      
      System.out.println("🚀 Server đã sẵn sàng nhận kết nối từ Client!");
      SocketServer socketServer = new SocketServer(dispatcher);
      socketServer.start();

    } catch (Exception e) {
      System.err.println("❌ LỖI NGHIÊM TRỌNG: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void initializeDemoData(
      RegistrationService registrationService, 
      UserService userService, 
      ItemService itemService, 
      AuctionService auctionService) throws Exception {

    // Tạo Admin
    Admin admin = registrationService.createAdmin("Admin Hệ Thống", "admin", "admin@auction.vn", "admin@123", "Nam", "1990-01-01");
    
    // Tạo Seller
    Seller seller = registrationService.registerSeller("Nguyễn Văn Bán", "seller@auction.vn", "seller@123", "Tech Store HCM");
    
    // Tạo Bidder
    Bidder bidder1 = registrationService.registerBidder("Trần Thị Mua", "bidder1@auction.vn", "bidder@123");
    Bidder bidder2 = registrationService.registerBidder("Lê Văn Đấu", "bidder2@auction.vn", "bidder@456");

    // Nạp tiền
    userService.depositForBidder(bidder1.getId(), 50_000_000);
    userService.depositForBidder(bidder2.getId(), 100_000_000);

    // Tạo sản phẩm
    Electronics laptop = ItemFactory.createElectronics(
        "MacBook Pro M3 2024", "Laptop cao cấp Apple M3 Pro", 25_000_000, 500_000,
        seller.getId(), "images/macbook.jpg", "Apple", "M3 Pro", 12, "LIKE_NEW");
    itemService.listItem(laptop);

    // Tạo phiên đấu giá (Bắt đầu sau 6 phút — tối thiểu 5 phút theo business rule)
    LocalDateTime start = LocalDateTime.now().plusMinutes(6);
    LocalDateTime end = start.plusHours(2);
    AuctionSession session = auctionService.createAuction(seller, laptop.getId(), start, end, 30);

    // Duyệt luôn
    auctionService.approveAuction(admin.getId(), session.getId());
    
    System.out.println("✅ Seed data hoàn tất.");
    System.out.println("👉 Admin: admin@auction.vn / admin@123");
    System.out.println("👉 Bidder: bidder1@auction.vn / bidder@123");
  }
}
