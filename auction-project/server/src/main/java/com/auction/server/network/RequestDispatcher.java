package com.auction.server.network;

import java.time.LocalDateTime;
import java.util.List;

import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.factory.ItemFactory;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.network.Request;
import com.auction.network.Response;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.DepositRequestService;
import com.auction.server.service.ItemService;
import com.auction.server.service.NotificationService;
import com.auction.server.service.RegistrationService;
import com.auction.server.service.UserService;
import com.auction.service.auction.AuctionSession;
import com.auction.service.auction.Bid;
import com.auction.enums.AuctionStatus;

/* class RequestDispatcher lam dieu huong request tu client 
nhan request -> phan tich actionType -> goi dung service -> tra ve response
logic nghiep vu o service , dispatcher lam nhiem vu trung chuyen du lieu */
public class RequestDispatcher {

  private final UserService userService;
  private final AuthService authService;
  private final RegistrationService registrationService;
  private final AuctionService auctionService;
  private final ItemService itemService;
  private final AutoBidService autoBidService;
  private final DepositRequestService depositRequestService;

  public RequestDispatcher(UserService userService, AuthService authService,
      RegistrationService registrationService,
      AuctionService auctionService, ItemService itemService, AutoBidService autoBidService,
      DepositRequestService depositRequestService) {
    this.userService = userService;
    this.authService = authService;
    this.registrationService = registrationService;
    this.auctionService = auctionService;
    this.itemService = itemService;
    this.autoBidService = autoBidService;
    this.depositRequestService = depositRequestService;
  }

  /**
   * Xử lý Request và trả về Response tương ứng.
   * Cần có thông tin id của người dùng đang gửi request (nếu đã login).
   */
  public Response dispatch(Request request, ClientHandler client) {
    try {
      return switch (request.getAction()) {
        case LOGIN -> handleLogin(request, client);
        case LOGOUT -> handleLogout(client);
        case REGISTER_USER -> handleRegisterUser(request);
        case UPGRADE_TO_SELLER -> handleUpgradeToSeller(request, client);
        case LIST_ITEM -> handleListItem(request, client);
        case CREATE_AUCTION -> handleCreateAuction(request, client);
        case GET_ACTIVE_AUCTIONS -> handleGetActiveAuctions();
        case GET_PENDING_AUCTIONS -> handleGetPendingAuctions(client);
        case GET_AUCTION_BIDS -> handleGetAuctionBids(request);
        case GET_MY_ITEMS -> handleGetMyItems(client);
        case GET_MY_AUCTIONS -> handleGetMyAuctions(client);
        case APPROVE_AUCTION -> handleApproveAuction(request, client);
        case REJECT_AUCTION -> handleRejectAuction(request, client);
        case CANCEL_AUCTION -> handleCancelAuction(request, client);
        case PLACE_BID -> handlePlaceBid(request, client);
        case GET_ALL_USERS -> handleGetAllUsers(client);
        case SET_USER_ACTIVE -> handleSetUserActive(request, client);
        case DEPOSIT_BALANCE -> handleDepositBalance(request, client);
        case SELF_DEPOSIT -> handleSelfDeposit(request, client);
        case REGISTER_AUTO_BID -> handleRegisterAutoBid(request, client);
        case REGISTER_NOTIFICATION -> handleRegisterNotification(request, client);
        case PING -> Response.success(ActionType.PING, "PONG");
        case GET_PENDING_DEPOSITS -> handleGetPendingDeposits(client);
        case APPROVE_DEPOSIT -> handleApproveDeposit(request, client);
        case REJECT_DEPOSIT -> handleRejectDeposit(request, client);
        case GET_AUCTION_RESULTS -> handleGetAuctionResults();
        default -> Response.error(request.getAction(), "Hành động chưa được hỗ trợ: " + request.getAction());
      };
    } catch (IllegalArgumentException | IllegalStateException e) {
      // Lỗi do người dùng nhập sai (ví dụ: mật khẩu sai, giá thấp quá...)
      return Response.error(request.getAction(), e.getMessage());
    } catch (Throwable e) {
      // Lỗi hệ thống hoặc thiếu thư viện (NoClassDefFoundError)
      e.printStackTrace();
      return Response.error(request.getAction(), "Lỗi máy chủ nội bộ: " + e.getMessage());
    }
  }

  private Response handleLogin(Request request, ClientHandler client) {
    Dto.LoginRequest payload = request.getPayloadAs(Dto.LoginRequest.class);
    if (payload == null) {
      throw new IllegalArgumentException("Payload bị thiếu");
    }

    // Gọi AuthService xử lý xác thực
    User user = authService.login(payload.email(), payload.password());

    // Đánh dấu Client này là user nào để check quyền sau này
    client.setLoggedInUserId(user.getId());
    com.auction.server.network.UserConnectionManager.getInstance().registerHandler(user.getId(), client);

    double balance = (user instanceof Seller s) ? s.getBalance()
        : (user instanceof Bidder b) ? b.getBalance()
            : 0.0;

    Dto.UserProfileResponse profile = new Dto.UserProfileResponse(
        user.getId(), user.getFullName(), user.getRole().name(), balance);

    return Response.success(ActionType.LOGIN, "Đăng nhập thành công", profile);
  }

  private Response handleLogout(ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn chưa đăng nhập");
    }
    com.auction.server.network.UserConnectionManager.getInstance().unregisterUser(client.getLoggedInUserId());
    client.setLoggedInUserId(null); // Xóa session
    return Response.success(ActionType.LOGOUT, "Đăng xuất thành công", null);
  }

  private Response handleRegisterUser(Request request) {
    Dto.RegisterUserRequest payload = request.getPayloadAs(Dto.RegisterUserRequest.class);
    if (payload == null) {
      throw new IllegalArgumentException("Dữ liệu đăng ký bị thiếu");
    }

    // 2. Gọi RegistrationService (mặc định thành Bidder)
    registrationService.registerUser(
        payload.fullName(), payload.username(), payload.email(),
        payload.password(), payload.gender(), payload.dateOfBirth());
    return Response.success(ActionType.REGISTER_USER, "Đăng ký thành công! Vui lòng đăng nhập.", null);
  }

  private Response handleUpgradeToSeller(Request request, ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập để nâng cấp");
    }

    Dto.UpgradeToSellerRequest payload = request.getPayloadAs(Dto.UpgradeToSellerRequest.class);
    if (payload == null) {
      throw new IllegalArgumentException("Thiếu thông tin nâng cấp");
    }

    registrationService.upgradeToSeller(client.getLoggedInUserId(), payload.shopName(), payload.citizenId());

    // Nâng cấp thành công, bắt client đăng xuất để reset session
    return Response.success(ActionType.UPGRADE_TO_SELLER, "Nâng cấp thành công. Vui lòng đăng nhập lại.", null);
  }

  private Response handleGetAuctionResults() {
    List<AuctionSession> sessions = auctionService.getFinishedAuctions();
    List<Dto.AuctionCardDto> dtos = sessions.stream()
        .map(s -> new Dto.AuctionCardDto(
            s.getId(),
            s.getItem().getId(),
            s.getItem().getName(),
            s.getItem().getDescription(),
            s.getItem().getStartingPrice(),
            s.getItem().getBidIncrement(),
            s.getCurrentPrice(),
            s.getCurrentWinnerId(),
            s.getCurrentWinnerName(),
            s.getSellerName(),
            s.getStatus().name(),
            s.getStartTime().toString(),
            s.getActualEndTime().toString(),
            s.getAntiSnipingSeconds(),
            s.getItem().getImageUrl(),
            s.getItem().getCategory() != null ? s.getItem().getCategory().name() : null,
            extractItemAttributes(s.getItem())))
        .toList();
    return Response.success(ActionType.GET_AUCTION_RESULTS, dtos);
  }

  private Response handleGetActiveAuctions() {
    List<AuctionSession> sessions = auctionService.getActiveAuctions();
    List<Dto.AuctionCardDto> dtos = sessions.stream()
        .map(s -> new Dto.AuctionCardDto(
            s.getId(),
            s.getItem().getId(),
            s.getItem().getName(),
            s.getItem().getDescription(),
            s.getItem().getStartingPrice(),
            s.getItem().getBidIncrement(),
            s.getCurrentPrice(),
            s.getCurrentWinnerId(),
            s.getCurrentWinnerName(),
            s.getSellerName(),
            s.getStatus().name(),
            s.getStartTime().toString(),
            s.getActualEndTime().toString(),
            s.getAntiSnipingSeconds(),
            s.getItem().getImageUrl(),
            s.getItem().getCategory() != null ? s.getItem().getCategory().name() : null,
            extractItemAttributes(s.getItem())))
        .toList();
    return Response.success(ActionType.GET_ACTIVE_AUCTIONS, dtos);
  }

  private Response handleGetAuctionBids(Request request) {
    Dto.SubscribeRequest payload = request.getPayloadAs(Dto.SubscribeRequest.class);
    if (payload == null || payload.sessionId() == null || payload.sessionId().isBlank()) {
      throw new IllegalArgumentException("Thiếu sessionId để lấy lịch sử bid");
    }
    List<Bid> bids = auctionService.getBidsBySession(payload.sessionId());
    return Response.success(ActionType.GET_AUCTION_BIDS, bids);
  }

  private Response handleGetPendingAuctions(ClientHandler client) {
    User admin = getAdminUserOrThrow(client);
    List<AuctionSession> sessions = auctionService.getAuctionsByStatus(AuctionStatus.PENDING);
    List<Dto.AuctionCardDto> dtos = sessions.stream()
        .map(s -> new Dto.AuctionCardDto(
            s.getId(),
            s.getItem().getId(),
            s.getItem().getName(),
            s.getItem().getDescription(),
            s.getItem().getStartingPrice(),
            s.getItem().getBidIncrement(),
            s.getCurrentPrice(),
            s.getCurrentWinnerId(),
            s.getCurrentWinnerName(),
            s.getSellerName(),
            s.getStatus().name(),
            s.getStartTime().toString(),
            s.getActualEndTime().toString(),
            s.getAntiSnipingSeconds(),
            s.getItem().getImageUrl(),
            s.getItem().getCategory() != null ? s.getItem().getCategory().name() : null,
            extractItemAttributes(s.getItem())))
        .toList();
    return Response.success(ActionType.GET_PENDING_AUCTIONS,
        "Lấy danh sách phiên chờ duyệt thành công cho " + admin.getFullName(), dtos);
  }

  private Response handleApproveAuction(Request request, ClientHandler client) {
    User admin = getAdminUserOrThrow(client);
    Dto.ApproveAuctionRequest payload = request.getPayloadAs(Dto.ApproveAuctionRequest.class);
    if (payload == null || payload.sessionId() == null || payload.sessionId().isBlank()) {
      throw new IllegalArgumentException("Thiếu sessionId để duyệt phiên");
    }

    auctionService.approveAuction(admin.getId(), payload.sessionId());
    return Response.success(ActionType.APPROVE_AUCTION, "Duyệt phiên thành công", null);
  }

  private Response handleRejectAuction(Request request, ClientHandler client) {
    User admin = getAdminUserOrThrow(client);
    Dto.RejectAuctionRequest payload = request.getPayloadAs(Dto.RejectAuctionRequest.class);
    if (payload == null || payload.sessionId() == null || payload.sessionId().isBlank()) {
      throw new IllegalArgumentException("Thiếu sessionId để từ chối phiên");
    }
    if (payload.reason() == null || payload.reason().isBlank()) {
      throw new IllegalArgumentException("Vui lòng nhập lý do từ chối");
    }

    auctionService.rejectAuction(admin.getId(), payload.sessionId(), payload.reason().trim());
    return Response.success(ActionType.REJECT_AUCTION, "Đã từ chối phiên đấu giá", null);
  }

  private Response handleCancelAuction(Request request, ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập");
    }
    User user = userService.findById(client.getLoggedInUserId())
        .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));

    Dto.CancelAuctionRequest payload = request.getPayloadAs(Dto.CancelAuctionRequest.class);
    if (payload == null || payload.sessionId() == null || payload.sessionId().isBlank()) {
      throw new IllegalArgumentException("Thiếu sessionId để huỷ phiên");
    }

    auctionService.cancelAuction(user, payload.sessionId());
    return Response.success(ActionType.CANCEL_AUCTION, "Đã huỷ phiên đấu giá thành công", null);
  }

  private Response handleGetMyItems(ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập");
    }

    User user = userService.findById(client.getLoggedInUserId())
        .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));

    if (!(user instanceof Seller)) {
      throw new IllegalStateException("Chỉ Seller mới có thể lấy danh sách sản phẩm của mình");
    }

    List<Item> items = itemService.getItemsBySeller(user.getId());
    List<com.auction.service.auction.AuctionSession> sellerSessions = auctionService.getAuctionsBySeller(user.getId());

    List<Dto.ItemResponse> itemResponses = items.stream()
        .map(item -> {
          String status = "AVAILABLE";
          if (!item.isAvailable()) {
            com.auction.service.auction.AuctionSession itemSession = sellerSessions.stream()
                .filter(s -> s.getItem().getId().equals(item.getId()))
                .max(java.util.Comparator.comparing(com.auction.service.auction.AuctionSession::getCreatedAt))
                .orElse(null);

            if (itemSession != null) {
              if (itemSession.getStatus() == com.auction.enums.AuctionStatus.FINISHED
                  && itemSession.getCurrentWinnerId() != null) {
                status = "SOLD";
              } else if (itemSession.getStatus() == com.auction.enums.AuctionStatus.CANCELLED
                  || (itemSession.getStatus() == com.auction.enums.AuctionStatus.FINISHED
                      && itemSession.getCurrentWinnerId() == null)) {
                status = "AVAILABLE";
              } else {
                status = "IN_AUCTION";
              }
            } else {
              status = "SOLD";
            }
          }
          return new Dto.ItemResponse(
              item.getId(),
              item.getName(),
              item.getDescription(),
              item.getStartingPrice(),
              status,
              item.getImageUrl());
        })
        .toList();

    return Response.success(ActionType.GET_MY_ITEMS, itemResponses);
  }

  private Response handlePlaceBid(Request request, ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập để đặt giá");
    }

    Dto.PlaceBidRequest payload = request.getPayloadAs(Dto.PlaceBidRequest.class);
    if (payload == null) {
      throw new IllegalArgumentException("Dữ liệu đặt giá bị thiếu");
    }

    User user = userService.findById(client.getLoggedInUserId())
        .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));

    if (!(user instanceof Bidder bidder)) {
      throw new IllegalStateException("Chỉ người mua hoặc người bán mới có thể đặt giá");
    }

    // Gọi AuctionService (hàm này đã có synchronized an toàn luồng)
    Bid bid = auctionService.placeBid(bidder, payload.sessionId(), payload.amount(), Bid.BidType.MANUAL);

    return Response.success(ActionType.PLACE_BID, "Đặt giá thành công!", bid);
  }

  private Response handleListItem(Request request, ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập để đăng sản phẩm");
    }

    Dto.ListItemRequest payload = request.getPayloadAs(Dto.ListItemRequest.class);
    if (payload == null) {
      throw new IllegalArgumentException("Dữ liệu sản phẩm bị thiếu");
    }

    User user = userService.findById(client.getLoggedInUserId())
        .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));

    if (!(user instanceof Seller seller)) {
      throw new IllegalStateException("Chỉ Seller mới có thể đăng sản phẩm");
    }

    java.util.Map<String, Object> extraData = new java.util.HashMap<>();
    switch (payload.category()) {
      case "ELECTRONICS" -> {
        extraData.put("brand", payload.brand());
        extraData.put("model", payload.model());
        extraData.put("warrantyMonths", payload.warrantyMonths());
        extraData.put("condition", payload.conditionStr());
      }
      case "ART" -> {
        extraData.put("artistName", payload.artistName());
        extraData.put("yearCreated", payload.creationYear());
        extraData.put("medium", payload.medium());
      }
      case "VEHICLE" -> {
        extraData.put("make", payload.make());
        extraData.put("model", payload.model());
        extraData.put("year", payload.year());
        extraData.put("mileage", payload.mileage());
        extraData.put("fuelType", payload.fuelType());
      }
      default -> throw new IllegalArgumentException("Danh mục không hợp lệ: " + payload.category());
    }

    Item item = ItemFactory.createNewItem(
        com.auction.enums.ItemCategory.valueOf(payload.category()),
        payload.name(), payload.description(), payload.startingPrice(), payload.minIncrement(),
        payload.imageUrl(), seller.getId(), extraData);

    itemService.listItem(item);

    return Response.success(ActionType.LIST_ITEM, "Đăng sản phẩm thành công!", null);
  }

  private Response handleCreateAuction(Request request, ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập để tạo phiên đấu giá");
    }

    Dto.CreateAuctionRequest payload = request.getPayloadAs(Dto.CreateAuctionRequest.class);
    if (payload == null) {
      throw new IllegalArgumentException("Dữ liệu phiên đấu giá bị thiếu");
    }

    User user = userService.findById(client.getLoggedInUserId())
        .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));

    if (!(user instanceof Seller seller)) {
      throw new IllegalStateException("Chỉ Seller mới có thể tạo phiên đấu giá");
    }

    LocalDateTime startTime = LocalDateTime.parse(payload.startTime());
    LocalDateTime endTime = LocalDateTime.parse(payload.endTime());

    AuctionSession session = auctionService.createAuction(seller, payload.itemId(), startTime, endTime,
        payload.antiSnipingSeconds());

    return Response.success(ActionType.CREATE_AUCTION, "Tạo phiên đấu giá thành công (chờ duyệt)!", session);
  }

  private Response handleGetMyAuctions(ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập");
    }
    User user = userService.findById(client.getLoggedInUserId())
        .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));
    if (!(user instanceof Seller)) {
      throw new IllegalStateException("Chỉ Seller mới có thể xem phiên đấu giá của mình");
    }
    List<AuctionSession> sessions = auctionService.getAuctionsBySeller(user.getId());
    List<Dto.AuctionSessionResponse> dtos = sessions.stream()
        .map(s -> new Dto.AuctionSessionResponse(
            s.getId(),
            s.getItem().getName(),
            s.getCurrentPrice(),
            s.getStatus().name(),
            s.getStartTime().toString(),
            s.getActualEndTime().toString()))
        .toList();
    return Response.success(ActionType.GET_MY_AUCTIONS, dtos);
  }

  private Response handleGetAllUsers(ClientHandler client) {
    getAdminUserOrThrow(client);
    List<com.auction.model.User> users = userService.getAllUsers();
    List<Dto.UserSummaryResponse> dtos = users.stream()
        .map(u -> new Dto.UserSummaryResponse(
            u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(), u.isActive()))
        .toList();
    return Response.success(ActionType.GET_ALL_USERS, dtos);
  }

  private Response handleSetUserActive(Request request, ClientHandler client) {
    User admin = getAdminUserOrThrow(client);
    Dto.SetUserActiveRequest payload = request.getPayloadAs(Dto.SetUserActiveRequest.class);
    if (payload == null || payload.targetUserId() == null || payload.targetUserId().isBlank()) {
      throw new IllegalArgumentException("Thiếu targetUserId");
    }
    userService.setUserActive(admin.getId(), payload.targetUserId(), payload.active());
    
    // Nếu khoá tài khoản (active == false), thông báo cho user nếu họ đang online
    if (!payload.active()) {
      ClientHandler targetHandler = com.auction.server.network.UserConnectionManager.getInstance().getHandler(payload.targetUserId());
      if (targetHandler != null) {
        // Gửi response thông báo bị khoá tài khoản tới Client của User đó
        targetHandler.sendResponse(Response.success(ActionType.SET_USER_ACTIVE, "LOCK_USER_NOTIFICATION", payload.targetUserId()));
        
        // Ngắt kết nối & xóa session trên server của user đó
        com.auction.server.network.UserConnectionManager.getInstance().unregisterUser(payload.targetUserId());
        targetHandler.setLoggedInUserId(null);
      }
    }

    String msg = payload.active() ? "Đã mở khoá tài khoản" : "Đã khoá tài khoản";
    return Response.success(ActionType.SET_USER_ACTIVE, msg, null);
  }

  private Response handleDepositBalance(Request request, ClientHandler client) {
    getAdminUserOrThrow(client);
    Dto.DepositRequest payload = request.getPayloadAs(Dto.DepositRequest.class);
    if (payload == null || payload.bidderId() == null || payload.bidderId().isBlank()) {
      throw new IllegalArgumentException("Thiếu bidderId");
    }
    if (payload.amount() <= 0) {
      throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
    }
    userService.depositForBidder(payload.bidderId(), payload.amount());
    return Response.success(ActionType.DEPOSIT_BALANCE,
        String.format("Đã nạp %.0f VND thành công", payload.amount()), null);
  }

  private Response handleSelfDeposit(Request request, ClientHandler client) {
    // 1. Kiểm tra đăng nhập
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập để nạp tiền");
    }

    // 2. Lấy dữ liệu (Dùng Dto.DepositRequest cho đồng bộ)
    Dto.DepositRequest payload = request.getPayloadAs(Dto.DepositRequest.class);
    if (payload == null || payload.amount() <= 0) {
      throw new IllegalArgumentException("Dữ liệu nạp tiền không hợp lệ");
    }

    // 3. Kiểm tra User có phải Bidder hoặc Seller không
    User user = userService.findById(client.getLoggedInUserId())
        .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));

    if (!(user instanceof Bidder || user instanceof Seller)) {
      throw new IllegalStateException("Chỉ Bidder hoặc Seller mới có thể gửi yêu cầu nạp tiền");
    }

    // 4. CHỖ QUAN TRỌNG: Đưa vào danh sách chờ duyệt thay vì nạp thẳng
    // Sử dụng biến depositRequestService đã khai báo ở Constructor của Dispatcher
    depositRequestService.requestDeposit(user.getId(), payload.amount());

    // 5. THÔNG BÁO CHO ADMIN (Để Admin thấy đơn mới hiện lên ngay)
    UserConnectionManager.getInstance().broadcastToAdmins(
        Response.success(ActionType.DEPOSIT_REQUEST_BROADCAST, "Có yêu cầu nạp tiền mới từ: " + user.getFullName(),
            null));

    // 6. Trả về thông báo cho User là "Đang chờ"
    return Response.success(ActionType.SELF_DEPOSIT,
        String.format("Yêu cầu nạp %.0f VND đã được gửi. Vui lòng đợi Admin phê duyệt!", payload.amount()),
        null);
  }

  private Response handleRegisterNotification(Request request, ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập để đăng ký nhận thông báo");
    }

    Dto.RegisterNotificationRequest payload = request.getPayloadAs(Dto.RegisterNotificationRequest.class);
    if (payload == null || payload.sessionId() == null || payload.sessionId().isBlank()) {
      throw new IllegalArgumentException("Thiếu sessionId để đăng ký nhận thông báo");
    }

    NotificationService.getInstance().registerNotification(payload.sessionId(),
        client.getLoggedInUserId());
    return Response.success(ActionType.REGISTER_NOTIFICATION, "Đã đăng ký nhận thông báo thành công!", null);
  }

  private Response handleRegisterAutoBid(Request request, ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập để đăng ký đấu giá tự động");
    }

    Dto.AutoBidConfigRequest payload = request.getPayloadAs(Dto.AutoBidConfigRequest.class);
    if (payload == null || payload.sessionId() == null || payload.sessionId().isBlank()) {
      throw new IllegalArgumentException("Dữ liệu cấu hình đấu giá tự động bị thiếu");
    }

    System.out.println("[RequestDispatcher] User " + client.getLoggedInUserId() + " registered auto bid for session "
        + payload.sessionId() + " with max budget " + payload.maxBudget());

    com.auction.service.auction.AutoBidConfig config = new com.auction.service.auction.AutoBidConfig(
        payload.sessionId(),
        client.getLoggedInUserId(),
        payload.maxBudget(),
        payload.customIncrement());
    autoBidService.registerAutoBid(config);

    return Response.success(ActionType.REGISTER_AUTO_BID,
        "Đăng ký đấu giá tự động thành công!", null);
  }

  private User getAdminUserOrThrow(ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập");
    }

    User user = userService.findById(client.getLoggedInUserId())
        .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));

    if (!(user instanceof Admin)) {
      throw new IllegalStateException("Chỉ Admin mới có quyền thực hiện thao tác này");
    }
    return user;
  }

  // 1. Lấy danh sách yêu cầu nạp tiền đang chờ duyệt
  private Response handleGetPendingDeposits(ClientHandler client) {
    getAdminUserOrThrow(client);
    // Lấy list từ service đã được tiêm vào qua Constructor
    var pendingRequests = depositRequestService.getPendingDeposits();
    return Response.success(ActionType.GET_PENDING_DEPOSITS, "Tải danh sách thành công", pendingRequests);
  }

  // 2. Admin phê duyệt nạp tiền
  @SuppressWarnings("unchecked")
  private Response handleApproveDeposit(Request request, ClientHandler client) {
    getAdminUserOrThrow(client);

    // SỬA TẠI ĐÂY: Thử đọc payload dưới dạng Map để lấy requestId
    java.util.Map<String, Object> payload = request.getPayloadAs(java.util.Map.class);
    String requestId = (String) payload.get("requestId");

    if (requestId == null || requestId.isBlank()) {
      throw new IllegalArgumentException("Thiếu ID yêu cầu phê duyệt");
    }

    // Lấy thông tin request trước khi xử lý duyệt (để biết số tiền và bidderId)
    var pendingReq = depositRequestService.getPendingRequest(requestId);
    if (pendingReq == null) {
      throw new IllegalArgumentException("Không tìm thấy yêu cầu nạp tiền (đã xử lý hoặc không tồn tại).");
    }
    String bidderId = pendingReq.bidderId();
    double amount = pendingReq.amount();

    // Gọi service để xử lý (Cộng tiền và xóa request)
    double newBalance = depositRequestService.approveDeposit(requestId);

    // Gửi phản hồi thời gian thực qua socket cho Bidder (nếu đang online) để cập
    // nhật số dư hiển thị
    ClientHandler bidderHandler = com.auction.server.network.UserConnectionManager.getInstance().getHandler(bidderId);
    if (bidderHandler != null) {
      Dto.DepositResultResponse result = new Dto.DepositResultResponse(newBalance);
      bidderHandler.sendResponse(Response.success(
          ActionType.DEPOSIT_BALANCE,
          String.format("Yêu cầu nạp tiền của bạn đã được phê duyệt! Số dư mới: %,.0f VND", newBalance),
          result));
    }

    // Thêm thông báo vào chuông cho Bidder (không cần hiển thị thông báo toàn cửa
    // sổ)
    NotificationService.getInstance().notifyDepositApproved(bidderId, amount);

    return Response.success(ActionType.APPROVE_DEPOSIT, "Phê duyệt nạp tiền thành công!", null);
  }

  // 3. Admin từ chối nạp tiền
  private Response handleRejectDeposit(Request request, ClientHandler client) {
    getAdminUserOrThrow(client);

    // Payload từ Admin gửi lên có thể là requestId (String)
    String requestId = request.getPayloadAs(String.class);
    if (requestId == null || requestId.isBlank()) {
      throw new IllegalArgumentException("Thiếu ID yêu cầu để từ chối");
    }

    depositRequestService.rejectDeposit(requestId, "Bị từ chối bởi Admin");
    return Response.success(ActionType.REJECT_DEPOSIT, "Đã từ chối yêu cầu nạp tiền.", null);
  }

  private java.util.Map<String, String> extractItemAttributes(Item item) {
    java.util.Map<String, String> attributes = new java.util.LinkedHashMap<>();
    if (item instanceof com.auction.model.Electronics e) {
      attributes.put("Thương hiệu", e.getBrand());
      attributes.put("Model", e.getModel());
      attributes.put("Tình trạng", e.getCondition() != null ? e.getCondition().getDisplay() : "N/A");
      attributes.put("Bảo hành", e.getWarrantyMonths() + " tháng");
    } else if (item instanceof com.auction.model.Art a) {
      attributes.put("Tác giả", a.getArtistName());
      attributes.put("Năm sáng tác", String.valueOf(a.getYearCreated()));
      attributes.put("Chất liệu", a.getMedium());
    } else if (item instanceof com.auction.model.Vehicle v) {
      attributes.put("Hãng xe", v.getMake());
      attributes.put("Dòng xe", v.getModel());
      attributes.put("Năm sản xuất", String.valueOf(v.getYear()));
      attributes.put("Số KM đã đi", String.valueOf(v.getMileage()));
      attributes.put("Nhiên liệu", v.getFuelType() != null ? v.getFuelType().getDisplay() : "N/A");
    }
    return attributes;
  }
}


