package com.auction.server.network;

import com.auction.common.dto.Dto;
import com.auction.common.model.auction.AuctionSession;
import com.auction.common.model.auction.AuctionStatus;
import com.auction.common.model.auction.Bid;
import com.auction.common.model.user.Admin;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import com.auction.common.factory.ItemFactory;
import com.auction.common.model.item.Item;
import com.auction.common.model.user.Seller;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuthService;
import com.auction.server.service.ItemService;
import com.auction.server.service.RegistrationService;
import com.auction.server.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * Class RequestDispatcher — Điều hướng Request từ Client
 * ============================================================
 *
 * Đóng vai trò giống như Controller trong mô hình MVC/REST.
 * Nhận `Request` -> Phân tích `ActionType` -> Gọi đúng `Service` -> Trả về
 * `Response`.
 *
 * Mọi Logic nghiệp vụ nên nằm ở Service, Dispatcher chỉ làm nhiệm vụ
 * "trung chuyển" dữ liệu (validate tham số mạng, gọi hàm, bắt lỗi).
 * ============================================================
 */
public class RequestDispatcher {

  private final UserService userService;
  private final AuthService authService;
  private final RegistrationService registrationService;
  private final AuctionService auctionService;
  private final ItemService itemService;
  private final com.auction.server.service.AutoBidService autoBidService;

  public RequestDispatcher(UserService userService, AuthService authService,
      RegistrationService registrationService,
      AuctionService auctionService, ItemService itemService,
      com.auction.server.service.AutoBidService autoBidService) {
    this.userService = userService;
    this.authService = authService;
    this.registrationService = registrationService;
    this.auctionService = auctionService;
    this.itemService = itemService;
    this.autoBidService = autoBidService;
  }

  /**
   * Xử lý Request và trả về Response tương ứng.
   * Cần có thông tin id của người dùng đang gửi request (nếu đã login).
   */
  public Response dispatch(Request request, ClientHandler client) {
    try {
      return switch (request.getAction()) {
        case LOGIN -> handleLogin(request, client);
        case ADMIN_LOGIN -> handleAdminLogin(request, client);
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

    // ===== BẢO MẬT PHÂN QUYỀN: User Login cổng =====
    // Tại cổng User Login, chỉ cho phép BIDDER và SELLER đăng nhập.
    // Admin buộc phải dùng cổng Admin Login riêng.
    if (user instanceof Admin) {
      throw new IllegalStateException("Tài khoản không có quyền truy cập cổng này. Vui lòng chọn cổng dành cho Quản trị viên!");
    }

    // Đánh dấu Client này là user nào để check quyền sau này
    client.setLoggedInUserId(user.getId());
    com.auction.server.network.UserConnectionManager.getInstance().registerUser(user.getId(), client);

    double balance = (user instanceof Seller s) ? s.getBalance()
                   : (user instanceof Bidder b) ? b.getDepositBalance()
                   : 0.0;

    Dto.UserProfileResponse profile = new Dto.UserProfileResponse(
        user.getId(), user.getFullName(), user.getRole().name(), balance);

    return Response.success(ActionType.LOGIN, "Đăng nhập thành công", profile);
  }

  /**
   * Xử lý đăng nhập cho cổng Admin Login.
   * Chỉ chấp nhận tài khoản có role ADMIN.
   * Nếu BIDDER/SELLER cố tình đăng nhập ở đây → báo lỗi phân quyền.
   */
  private Response handleAdminLogin(Request request, ClientHandler client) {
    Dto.LoginRequest payload = request.getPayloadAs(Dto.LoginRequest.class);
    if (payload == null) {
      throw new IllegalArgumentException("Payload bị thiếu");
    }

    // Gọi AuthService xử lý xác thực
    User user = authService.login(payload.email(), payload.password());

    // ===== BẢO MẬT PHÂN QUYỀN: Admin Login cổng =====
    // Tại cổng Admin Login, chỉ cho phép ADMIN đăng nhập.
    if (!(user instanceof Admin)) {
      throw new IllegalStateException("Tài khoản không có quyền quản trị!");
    }

    // Đánh dấu Client này là user nào để check quyền sau này
    client.setLoggedInUserId(user.getId());
    com.auction.server.network.UserConnectionManager.getInstance().registerUser(user.getId(), client);

    double balance = 0.0; // Admin không có balance

    Dto.UserProfileResponse profile = new Dto.UserProfileResponse(
        user.getId(), user.getFullName(), user.getRole().name(), balance);

    return Response.success(ActionType.ADMIN_LOGIN, "Đăng nhập Admin thành công", profile);
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

  private String extractConditionStr(Item item) {
    if (item instanceof com.auction.common.model.item.Electronics elec) {
      return elec.getCondition() != null ? elec.getCondition().getDisplayName() : "N/A";
    }
    // Art and Vehicle use string-based condition
    return "Mới";
  }

  private String extractMaterial(Item item) {
    return switch (item.getCategory()) {
      case ELECTRONICS -> ((com.auction.common.model.item.Electronics) item).getBrand();
      case ART -> ((com.auction.common.model.item.Art) item).getMedium();
      case VEHICLE -> ((com.auction.common.model.item.Vehicle) item).getFuelType().getDisplayName();
      default -> "N/A";
    };
  }

  private int extractWarrantyMonths(Item item) {
    return switch (item.getCategory()) {
      case ELECTRONICS -> ((com.auction.common.model.item.Electronics) item).getWarrantyMonths();
      case ART -> 0;
      case VEHICLE -> ((com.auction.common.model.item.Vehicle) item).getYear();
      default -> 0;
    };
  }

  private Dto.AuctionCardDto mapSessionToDto(AuctionSession s) {
    Item item = s.getItem();
    return new Dto.AuctionCardDto(
        s.getId(),
        item.getId(),
        item.getName(),
        item.getDescription(),
        item.getBasePrice(),
        item.getMinIncrement(),
        s.getCurrentPrice(),
        s.getCurrentWinnerId(),
        s.getCurrentWinnerName(),
        s.getSellerName(),
        s.getStatus().name(),
        s.getStartTime().toString(),
        s.getActualEndTime().toString(),
        s.getAntiSnipingSeconds(),
        item.getCategory().name(),
        item.getImageUrl(),
        extractConditionStr(item),
        extractMaterial(item),
        extractWarrantyMonths(item)
    );
  }

  private Response handleGetActiveAuctions() {
    List<AuctionSession> sessions = auctionService.getActiveAuctions();
    List<Dto.AuctionCardDto> dtos = sessions.stream()
        .map(this::mapSessionToDto)
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
        .map(this::mapSessionToDto)
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
    
    // Broadcast để các Dashboard tự cập nhật
    for (ClientHandler h : com.auction.server.network.UserConnectionManager.getInstance().getActiveHandlers()) {
        h.sendResponse(Response.success(ActionType.NEW_AUCTION_BROADCAST, "Phiên đấu giá đã được duyệt", null));
    }
    
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
    
    for (ClientHandler h : com.auction.server.network.UserConnectionManager.getInstance().getActiveHandlers()) {
        h.sendResponse(Response.success(ActionType.NEW_AUCTION_BROADCAST, "Phiên đấu giá đã bị từ chối", null));
    }
    
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
    
    for (ClientHandler h : com.auction.server.network.UserConnectionManager.getInstance().getActiveHandlers()) {
        h.sendResponse(Response.success(ActionType.NEW_AUCTION_BROADCAST, "Phiên đấu giá đã bị huỷ", null));
    }
    
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
    
    // Map to DTO
    List<Dto.ItemResponse> itemResponses = items.stream()
        .map(item -> new Dto.ItemResponse(
            item.getId(),
            item.getName(),
            item.getDescription(),
            item.getBasePrice(),
            item.isAvailable() ? "AVAILABLE" : "SOLD",
            item.getImageUrl()
        ))
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
      throw new IllegalStateException("Chỉ Bidder mới có thể đặt giá");
    }

    // Gọi AuctionService (hàm này đã có synchronized an toàn luồng)
    Bid bid = auctionService.placeBid(bidder, payload.sessionId(), payload.amount());

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

    Item item = switch (payload.category()) {
      case "ELECTRONICS" -> ItemFactory.createElectronics(
          payload.name(), payload.description(), payload.basePrice(), payload.minIncrement(),
          seller.getId(), payload.imageUrl(), payload.brand(), payload.model(), payload.warrantyMonths(), payload.conditionStr()
      );
      case "ART" -> ItemFactory.createArt(
          payload.name(), payload.description(), payload.basePrice(), payload.minIncrement(),
          seller.getId(), payload.imageUrl(), payload.artistName(), payload.creationYear(), payload.medium(), payload.authenticated(), payload.certificateId(), payload.dimensions()
      );
      case "VEHICLE" -> ItemFactory.createVehicle(
          payload.name(), payload.description(), payload.basePrice(), payload.minIncrement(),
          seller.getId(), payload.imageUrl(), payload.vehicleType(), payload.make(), payload.model(), payload.year(), payload.mileage(), payload.fuelType(), payload.transmission(), payload.color(), payload.licensePlate(), payload.hasValidRegistry()
      );
      default -> throw new IllegalArgumentException("Danh mục không hợp lệ: " + payload.category());
    };

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

    AuctionSession session = auctionService.createAuction(seller, payload.itemId(), startTime, endTime, payload.antiSnipingSeconds());

    // Bắn broadcast để Admin tự cập nhật danh sách
    for (ClientHandler h : com.auction.server.network.UserConnectionManager.getInstance().getActiveHandlers()) {
        h.sendResponse(Response.success(ActionType.NEW_AUCTION_BROADCAST, "Có phiên đấu giá mới cần duyệt", null));
    }

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
            s.getActualEndTime().toString()
        ))
        .toList();
    return Response.success(ActionType.GET_MY_AUCTIONS, dtos);
  }

  private Response handleGetAllUsers(ClientHandler client) {
    getAdminUserOrThrow(client);
    List<com.auction.common.model.user.User> users = userService.getAllUsers();
    List<Dto.UserSummaryResponse> dtos = users.stream()
        .map(u -> new Dto.UserSummaryResponse(
            u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(), u.isActive()
        ))
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
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Бạn cần đăng nhập để nạp tiền");
    }

    Dto.SelfDepositRequest payload = request.getPayloadAs(Dto.SelfDepositRequest.class);
    if (payload == null) {
      throw new IllegalArgumentException("Dữ liệu nạp tiền bị thiếu");
    }
    if (payload.amount() <= 0) {
      throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
    }

    // Kiểm tra user là Bidder
    User user = userService.findById(client.getLoggedInUserId())
        .orElseThrow(() -> new IllegalStateException("Бài khoản không tồn tại"));
    if (!(user instanceof Bidder bidder)) {
      throw new IllegalStateException("Chỉ Bidder mới có thể nạp tiền");
    }

    userService.depositForBidder(bidder.getId(), payload.amount());

    // Tải lại bidder để lấy số dư mới
    Bidder updated = (Bidder) userService.findById(bidder.getId()).get();
    Dto.DepositResultResponse result = new Dto.DepositResultResponse(updated.getDepositBalance());

    return Response.success(ActionType.SELF_DEPOSIT,
        String.format("Đã nạp %.0f VND thành công! Số dư hiện tại: %.0f VND",
            payload.amount(), updated.getDepositBalance()), result);
  }

  private Response handleRegisterAutoBid(Request request, ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập để thiết lập đấu giá tự động");
    }

    Dto.AutoBidConfigRequest payload = request.getPayloadAs(Dto.AutoBidConfigRequest.class);
    if (payload == null) {
      throw new IllegalArgumentException("Dữ liệu cấu hình bị thiếu");
    }

    com.auction.common.model.auction.AutoBidConfig config = new com.auction.common.model.auction.AutoBidConfig(
        payload.sessionId(),
        client.getLoggedInUserId(),
        payload.maxBudget(),
        payload.strategyType()
    );

    autoBidService.registerAutoBid(config);
    return Response.success(ActionType.REGISTER_AUTO_BID, "Đã kích hoạt đấu giá tự động thành công!", null);
  }

  private Response handleRegisterNotification(Request request, ClientHandler client) {
    if (client.getLoggedInUserId() == null) {
      throw new IllegalStateException("Bạn cần đăng nhập để đăng ký nhận thông báo");
    }

    Dto.RegisterNotificationRequest payload = request.getPayloadAs(Dto.RegisterNotificationRequest.class);
    if (payload == null || payload.sessionId() == null || payload.sessionId().isBlank()) {
      throw new IllegalArgumentException("Thiếu sessionId để đăng ký nhận thông báo");
    }

    com.auction.server.service.NotificationService.getInstance().registerNotification(payload.sessionId(), client.getLoggedInUserId());
    return Response.success(ActionType.REGISTER_NOTIFICATION, "Đã đăng ký nhận thông báo thành công!", null);
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
}
