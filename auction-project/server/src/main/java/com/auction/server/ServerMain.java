package com.auction.server;

import java.time.LocalDateTime;
import java.util.Map;

import com.auction.factory.ItemFactory;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Seller;
import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.database.DatabaseManager;
import com.auction.server.network.RequestDispatcher;
import com.auction.server.network.SocketServer;
import com.auction.server.scheduler.AuctionTimerManager;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.ItemService;
import com.auction.server.service.RegistrationService;
import com.auction.server.service.UserService;
import com.auction.service.auction.AuctionSession;
import com.auction.server.service.DepositRequestService;

public class ServerMain {

  public static void main(String[] args) {
    System.out.println("Đang khởi động Hệ thống Đấu giá Trực tuyến...");

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
      DepositRequestService depositRequestService = new DepositRequestService(userService);
      // 3. Khởi tạo Timer
      AuctionTimerManager timerManager = AuctionTimerManager.getInstance();
      timerManager.initialize(auctionService);
      timerManager.start();

      // 4. Shutdown Hook
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\n Đang tắt Server an toàn...");
        timerManager.stop();
        db.close();
      }));

      // 5. Seed Data (Dữ liệu mẫu)
      if (userDAO.count() == 0) {
        System.out.println("📦 Database trống. Đang khởi tạo dữ liệu mẫu...");
        initializeDemoData(registrationService, userService, itemService, auctionService);
      } else {
        System.out.println("Database đã có dữ liệu. Bỏ qua bước tạo mẫu.");
      }

      // 6. Khởi động Socket Server
      RequestDispatcher dispatcher = new RequestDispatcher(
          userService, authService, registrationService, auctionService, itemService, autoBidService,
          depositRequestService);

      System.out.println("Server đã sẵn sàng nhận kết nối từ Client!");
      SocketServer socketServer = new SocketServer(dispatcher);
      socketServer.start();

    } catch (Exception e) {
      System.err.println("LỖI NGHIÊM TRỌNG: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void initializeDemoData(
      RegistrationService registrationService,
      UserService userService,
      ItemService itemService,
      AuctionService auctionService) throws Exception {

    // Tạo Admin
    Admin admin = registrationService.createAdmin("Admin Hệ Thống", "admin", "admin@auction.vn");

    // Tạo Seller
    Seller seller = registrationService.registerSeller("Nguyễn Văn Bán", "seller@auction.vn", "seller@123",
        "Tech Store HCM");

    // Tạo Bidder
    Bidder bidder1 = registrationService.registerBidder("Trần Thị Mua", "bidder1@auction.vn", "bidder@123");
    Bidder bidder2 = registrationService.registerBidder("Lê Văn Đấu", "bidder2@auction.vn", "bidder@456");

    // Nạp tiền
    userService.depositForBidder(bidder1.getId(), 50_000_000);
    userService.depositForBidder(bidder2.getId(), 100_000_000);

    Map<String, Object> extraData = new java.util.HashMap<>();
    extraData.put("brand", "Apple");
    extraData.put("model", "M3 Pro");
    extraData.put("warrantyMonths", 12);
    extraData.put("condition", "LIKE_NEW");

    Electronics laptop = (Electronics) ItemFactory.createNewItem(
        com.auction.enums.ItemCategory.ELECTRONICS,
        "MacBook Pro M3 2024", "Laptop cao cấp Apple M3 Pro", 25_000_000, 500_000,
        "images/macbook.jpg", seller.getId(), extraData);
    itemService.listItem(laptop);
    // Tạo phiên đấu giá (Bắt đầu sau 6 phút — tối thiểu 5 phút theo business rule)
    LocalDateTime start = LocalDateTime.now().plusMinutes(0);
    LocalDateTime end = start.plusHours(2);
    AuctionSession session = auctionService.createAuction(seller, laptop.getId(), start, end, 30);

    // Duyệt luôn
    auctionService.approveAuction(admin.getId(), session.getId());

    System.out.println("Seed data hoàn tất.");
  }
}
