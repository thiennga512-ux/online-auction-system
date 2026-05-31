package com.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.ClientMain;
import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.network.Request;
import com.network.SocketClient;
import com.util.SessionManager;
import com.util.TimeFormatUtil;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class HomeController implements LifeCycleAwareController {

  @FXML
  private FlowPane productsGrid;

  // Search & Category UI (new home.fxml)
  @FXML
  private ScrollPane mainScrollPane;
  @FXML
  private Label welcomeMessageLabel;
  @FXML
  private TextField searchInput;
  @FXML
  private Button searchButton;
  @FXML
  private Label searchErrorLabel;
  @FXML
  private VBox searchContainer;
  @FXML
  private HBox categoryRow;
  @FXML
  private VBox allCategoryItem;
  @FXML
  private VBox electronicsCategoryItem;
  @FXML
  private VBox artCategoryItem;
  @FXML
  private VBox vehicleCategoryItem;

  private final Consumer<com.auction.network.Response> responseListener = this::handleResponse;

  // Lưu label đếm ngược theo sessionId
  private final Map<String, Label> countdownLabelsBySessionId = new HashMap<>();
  // Lưu trạng thái (OPEN / RUNNING) theo sessionId
  private final Map<String, String> statusBySessionId = new HashMap<>();
  // Lưu startTime (dùng khi OPEN)
  private final Map<String, LocalDateTime> startTimeBySessionId = new HashMap<>();
  // Lưu actualEndTime (dùng khi RUNNING)
  private final Map<String, LocalDateTime> endTimeBySessionId = new HashMap<>();

  // State
  private List<Dto.AuctionCardDto> allSessions = new java.util.ArrayList<>();
  private String currentFilter = "ALL"; // ALL, OPEN, RUNNING, FINISHED, ELECTRONICS, ART, VEHICLE
  private String searchKeyword = "";

  // Old filter buttons (không còn trong FXML mới nhưng dùng @FXML nullable)
  @FXML
  private Button filterAllBtn;
  @FXML
  private Button filterOpenBtn;
  @FXML
  private Button filterRunningBtn;
  @FXML
  private Button filterFinishedBtn;

  private Timeline countdownTimeline;

  @FXML
  public void initialize() {
    SocketClient.getInstance().removeListener(responseListener);
    SocketClient.getInstance().addListener(responseListener);

    // Cập nhật welcome message theo trạng thái đăng nhập
    if (welcomeMessageLabel != null) {
      Dto.UserProfileResponse user = SessionManager.getInstance().getCurrentUser();
      if (user != null) {
        welcomeMessageLabel.setText("Xin chào, " + user.fullName() + "! Chào mừng trở lại.");
      } else {
        welcomeMessageLabel.setText("Đăng nhập để tham gia đặt giá ngay hôm nay!");
      }
    }

    // Gắn sự kiện cho nút tìm kiếm
    if (searchButton != null) {
      searchButton.setOnAction(e -> handleSearch());
    }
    // Gắn Enter cho ô tìm kiếm
    if (searchInput != null) {
      searchInput.setOnAction(e -> handleSearch());
    }

    renderLoadingState();
    refreshActiveAuctions();
  }

  private void refreshActiveAuctions() {
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ACTIVE_AUCTIONS, null));
  }

  private void handleResponse(com.auction.network.Response response) {
    if (response.getActionType() == ActionType.GET_ACTIVE_AUCTIONS) {
      if (!response.isSuccess()) {
        showError("Không thể tải danh sách phiên đấu giá", response.getMessage());
        return;
      }
      List<Dto.AuctionCardDto> sessions = response.getDataAsList(Dto.AuctionCardDto.class);
      allSessions = new java.util.ArrayList<>(sessions);
      applyFilter();
      return;
    }

    if (response.getActionType() == ActionType.PLACE_BID) {
      if (response.isSuccess()) {
        showInfo("Đặt giá thành công", response.getMessage());
        refreshActiveAuctions();
      } else {
        showError("Đặt giá thất bại", response.getMessage());
      }
    }
  }

  private void renderLoadingState() {
    productsGrid.getChildren().clear();
    productsGrid.getChildren().add(createInfoCard("Đang tải phiên đấu giá..."));
  }

  private void renderAuctions(List<Dto.AuctionCardDto> sessions) {
    Platform.runLater(() -> {
      productsGrid.getChildren().clear();
      countdownLabelsBySessionId.clear();
      statusBySessionId.clear();
      startTimeBySessionId.clear();
      endTimeBySessionId.clear();

      if (sessions == null || sessions.isEmpty()) {
        productsGrid.getChildren().add(createInfoCard("Hiện chưa có phiên đấu giá nào đang mở."));
        stopCountdownTimer();
        return;
      }

      for (Dto.AuctionCardDto session : sessions) {
        try {
          productsGrid.getChildren().add(createAuctionCard(session));
        } catch (Exception e) {
          System.err
              .println("[HomeController] Lỗi hiển thị thẻ đấu giá " + session.sessionId() + ": " + e.getMessage());
          e.printStackTrace();
        }
      }
      startCountdownTimer();
    });
  }

  // -------------------------------------------------------
  // FILTERING & SEARCH
  // -------------------------------------------------------

  @FXML
  private void handleFilterAll() {
    currentFilter = "ALL";
    applyFilter();
  }

  @FXML
  private void handleFilterOpen() {
    currentFilter = "OPEN";
    applyFilter();
  }

  @FXML
  private void handleFilterRunning() {
    currentFilter = "RUNNING";
    applyFilter();
  }

  @FXML
  private void handleFilterFinished() {
    currentFilter = "FINISHED";
    applyFilter();
  }

  /** Handler cho click vào category circle (ALL, ELECTRONICS, ART, VEHICLE) */
  @FXML
  private void onCategoryClicked(MouseEvent event) {
    if (event.getSource() instanceof VBox clickedVBox) {
      Object userData = clickedVBox.getUserData();
      if (userData instanceof String category) {
        currentFilter = category;
        searchKeyword = ""; // reset search khi đổi category
        if (searchInput != null)
          searchInput.clear();
        if (searchErrorLabel != null)
          searchErrorLabel.setText("");
        updateCategoryItemStyles(clickedVBox);
        applyFilter();
      }
    }
  }

  private void handleSearch() {
    if (searchInput == null)
      return;
    searchKeyword = searchInput.getText().trim().toLowerCase();
    if (searchErrorLabel != null)
      searchErrorLabel.setText("");
    
    applyFilter();

    if (!searchKeyword.isEmpty()) {
      int matchCount = 0;
      for (Dto.AuctionCardDto session : allSessions) {
        boolean matchStatus = "ALL".equals(currentFilter)
            || currentFilter.equals(session.status())
            || currentFilter.equals(session.category());
        boolean matchSearch = (session.itemName() != null && session.itemName().toLowerCase().contains(searchKeyword))
            || (session.itemDescription() != null && session.itemDescription().toLowerCase().contains(searchKeyword))
            || (session.sellerName() != null && session.sellerName().toLowerCase().contains(searchKeyword));
        
        if (matchStatus && matchSearch) {
          matchCount++;
        }
      }
      
      if (matchCount > 0) {
        if (searchErrorLabel != null) {
          searchErrorLabel.setText("Đã tìm thấy " + matchCount + " phiên đấu giá phù hợp.");
          searchErrorLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-padding: 0 24;");
        }
      } else {
        if (searchErrorLabel != null) {
          searchErrorLabel.setText("Không tìm thấy phiên đấu giá nào phù hợp với từ khóa: " + searchKeyword);
          searchErrorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 0 24;");
        }
      }
    }
  }

  private void updateCategoryItemStyles(VBox selected) {
    VBox[] items = { allCategoryItem, electronicsCategoryItem, artCategoryItem, vehicleCategoryItem };
    for (VBox item : items) {
      if (item == null)
        continue;
      if (item == selected) {
        if (!item.getStyleClass().contains("category-item-selected"))
          item.getStyleClass().add("category-item-selected");
      } else {
        item.getStyleClass().remove("category-item-selected");
      }
    }
  }

  private void applyFilter() {
    List<Dto.AuctionCardDto> filtered = new java.util.ArrayList<>();
    for (Dto.AuctionCardDto session : allSessions) {
      // Lọc theo category (status cũ: ALL/OPEN/RUNNING/FINISHED; category mới:
      // ELECTRONICS/ART/VEHICLE)
      boolean matchStatus = "ALL".equals(currentFilter)
          || currentFilter.equals(session.status())
          || currentFilter.equals(session.category());

      // Lọc theo từ khoá tìm kiếm
      boolean matchSearch = searchKeyword.isEmpty()
          || (session.itemName() != null && session.itemName().toLowerCase().contains(searchKeyword))
          || (session.itemDescription() != null && session.itemDescription().toLowerCase().contains(searchKeyword))
          || (session.sellerName() != null && session.sellerName().toLowerCase().contains(searchKeyword));

      if (matchStatus && matchSearch) {
        filtered.add(session);
      }
    }
    updateFilterButtonStyles();
    renderAuctions(filtered);
  }

  private void updateFilterButtonStyles() {
    String activeStyle = "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;";
    String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-border-color: #4b5563; -fx-border-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;";

    if (filterAllBtn != null)
      filterAllBtn.setStyle("ALL".equals(currentFilter) ? activeStyle : inactiveStyle);
    if (filterOpenBtn != null)
      filterOpenBtn.setStyle("OPEN".equals(currentFilter) ? activeStyle : inactiveStyle);
    if (filterRunningBtn != null)
      filterRunningBtn.setStyle("RUNNING".equals(currentFilter) ? activeStyle : inactiveStyle);
    if (filterFinishedBtn != null)
      filterFinishedBtn.setStyle("FINISHED".equals(currentFilter) ? activeStyle : inactiveStyle);
  }

  // -------------------------------------------------------
  // TẠO CARD ĐẤU GIÁ
  // -------------------------------------------------------

  private VBox createAuctionCard(Dto.AuctionCardDto session) {
    VBox card = new VBox(10);
    card.getStyleClass().add("card-container");
    card.setPadding(new Insets(20));
    card.setPrefWidth(260);

    // ===== ẢNH SẢN PHẨM =====
    javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
    imgView.setFitHeight(160);
    imgView.setFitWidth(220);
    imgView.setPreserveRatio(true);
    loadImage(imgView, session.imageUrl());

    // Bọc ảnh vào VBox có chiều cao cố định để không bị lệch thẻ
    javafx.scene.layout.VBox imgContainer = new javafx.scene.layout.VBox(imgView);
    imgContainer.setAlignment(javafx.geometry.Pos.CENTER);
    imgContainer.setMinHeight(160);
    imgContainer.setMaxHeight(160);

    // ===== BADGE TRẠNG THÁI =====
    String status = session.status();
    Label statusBadge = createStatusBadge(status);

    // ===== TÊN SẢN PHẨM =====
    Label nameLabel = new Label(session.itemName());
    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
    nameLabel.setWrapText(true);
    nameLabel.setMinHeight(44); // Đủ chỗ cho tối đa 2 dòng tên SP
    nameLabel.setMaxHeight(44);
    nameLabel.setAlignment(javafx.geometry.Pos.TOP_LEFT);

    // ===== MÔ TẢ SẢN PHẨM (tóm tắt) =====
    String rawDesc = session.itemDescription();
    String shortDesc = (rawDesc != null && !rawDesc.isBlank())
        ? (rawDesc.length() > 65 ? rawDesc.substring(0, 65).trim() + "…" : rawDesc)
        : "Không có mô tả";
    Label descLabel = new Label(shortDesc);
    descLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
    descLabel.setWrapText(true);
    descLabel.setMinHeight(36); // Đủ chỗ cho 2 dòng mô tả
    descLabel.setMaxHeight(36);
    descLabel.setAlignment(javafx.geometry.Pos.TOP_LEFT);

    // ===== GIÁ HIỆN TẠI =====
    String formattedPrice = String.format("%,.0f", session.currentPrice());
    Label priceLabel = new Label("Giá hiện tại: " + formattedPrice + " VND");
    priceLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 14px;");

    // ===== COUNTDOWN LABEL (thay đổi tuỳ status) =====
    LocalDateTime startTime = LocalDateTime.parse(session.startTime());
    LocalDateTime endTime = LocalDateTime.parse(session.actualEndTime());

    Label countdownLabel = buildCountdownLabel(status, startTime, endTime);

    // Đăng ký vào map để Timer cập nhật mỗi giây
    countdownLabelsBySessionId.put(session.sessionId(), countdownLabel);
    statusBySessionId.put(session.sessionId(), status);
    startTimeBySessionId.put(session.sessionId(), startTime);
    endTimeBySessionId.put(session.sessionId(), endTime);

    // ===== THÔNG TIN BỔ SUNG =====
    Label sellerLabel = new Label("👤 Người bán: " + session.sellerName());
    sellerLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

    // Hiện người dẫn đầu nếu đang RUNNING
    Label leaderLabel = buildLeaderLabel(session, status);

    // ===== THỜI GIAN MỞ / KẾT THÚC =====
    VBox scheduleBox = buildScheduleBox(status, startTime, endTime);

    // ===== NÚT HÀNH ĐỘNG =====
    Button auctionBtn = new Button("🏷 ĐẤU GIÁ");
    auctionBtn.getStyleClass().add("primary-button");
    auctionBtn.setMaxWidth(Double.MAX_VALUE);
    auctionBtn.setPrefHeight(36);

    boolean isRunning = "RUNNING".equals(status);
    boolean isFinished = "FINISHED".equals(status);

    if (isFinished) {
      auctionBtn.setText("XEM KẾT QUẢ");
      auctionBtn.setStyle(
          "-fx-background-color: #4b5563; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
      // Chuyển sang trang kết quả đấu giá
      auctionBtn.setOnAction(e -> ClientMain.getMainController().switchContent("auction_results.fxml"));
    } else if (!isRunning) {
      auctionBtn.setText("XEM TRƯỚC (CHƯA MỞ)");
      auctionBtn.setStyle(
          "-fx-background-color: #60a5fa; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
      auctionBtn.setOnAction(e -> goToAuctionDetail(session.sessionId()));
    } else {
      auctionBtn.setOnAction(e -> goToAuctionDetail(session.sessionId()));
    }

    card.getChildren().addAll(imgContainer, statusBadge, nameLabel, descLabel, priceLabel,
        countdownLabel, scheduleBox, sellerLabel, leaderLabel, auctionBtn);
    return card;
  }

  // -------------------------------------------------------
  // CÁC HELPER TẠO COMPONENT
  // -------------------------------------------------------

  /**
   * Tạo badge màu tương ứng với trạng thái phiên.
   * OPEN = xanh lam | RUNNING = đỏ nhấp nháy
   */
  private Label createStatusBadge(String status) {
    Label badge = new Label();
    if ("RUNNING".equals(status)) {
      badge.setText("🔴 ĐANG ĐẤU GIÁ");
      badge.setStyle(
          "-fx-background-color: rgba(239,68,68,0.2);" +
              "-fx-border-color: #ef4444;" +
              "-fx-border-radius: 20;" +
              "-fx-background-radius: 20;" +
              "-fx-text-fill: #f87171;" +
              "-fx-font-size: 11px;" +
              "-fx-font-weight: bold;" +
              "-fx-padding: 4 10;");
    } else if ("FINISHED".equals(status)) {
      badge.setText("⚫ ĐÃ KẾT THÚC");
      badge.setStyle(
          "-fx-background-color: rgba(107,114,128,0.2);" +
              "-fx-border-color: #6b7280;" +
              "-fx-border-radius: 20;" +
              "-fx-background-radius: 20;" +
              "-fx-text-fill: #9ca3af;" +
              "-fx-font-size: 11px;" +
              "-fx-font-weight: bold;" +
              "-fx-padding: 4 10;");
    } else {
      badge.setText("🟢 SẮP MỞ");
      badge.setStyle(
          "-fx-background-color: rgba(59,130,246,0.15);" +
              "-fx-border-color: #3b82f6;" +
              "-fx-border-radius: 20;" +
              "-fx-background-radius: 20;" +
              "-fx-text-fill: #93c5fd;" +
              "-fx-font-size: 11px;" +
              "-fx-font-weight: bold;" +
              "-fx-padding: 4 10;");
    }
    return badge;
  }

  /**
   * Tạo Label đếm ngược chính.
   * - OPEN → "⏳ Mở sau: X giờ Y phút Z giây" (xanh lam)
   * - RUNNING → "⏱ Kết thúc sau: X phút Y giây" (xanh ngọc / vàng / đỏ)
   */
  private Label buildCountdownLabel(String status, LocalDateTime startTime, LocalDateTime endTime) {
    Label label = new Label();
    label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
    label.setWrapText(true);
    refreshCountdownLabel(label, status, startTime, endTime);
    return label;
  }

  private void refreshCountdownLabel(Label label, String status,
      LocalDateTime startTime, LocalDateTime endTime) {
    if ("RUNNING".equals(status)) {
      String color = TimeFormatUtil.getEndCountdownColor(endTime);
      LocalDateTime now = LocalDateTime.now();
      if (now.isAfter(endTime)) {
        label.setText("⏱ Đã kết thúc");
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
      } else {
        label.setText("⏱ Còn lại: " + TimeFormatUtil.formatRemainingTime(endTime));
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
      }
    } else if ("FINISHED".equals(status)) {
      label.setText("⏱ Đã kết thúc");
      label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
    } else {
      // OPEN — đếm ngược đến khi mở
      String color = TimeFormatUtil.getOpenCountdownColor(startTime);
      LocalDateTime now = LocalDateTime.now();
      if (now.isAfter(startTime)) {
        label.setText("⏳ Chuẩn bị bắt đầu...");
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #60a5fa;");
      } else {
        label.setText("⏳ Mở sau: " + TimeFormatUtil.formatRemainingTime(startTime));
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
      }
    }
  }

  private VBox buildScheduleBox(String status, LocalDateTime startTime, LocalDateTime endTime) {
    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    String openStr = startTime.format(fmt);
    String closeStr = endTime.format(fmt);

    VBox box = new VBox(4);
    Label startLbl = new Label("🕐 Bắt đầu: " + openStr);
    startLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
    Label endLbl = new Label("🏁 Kết thúc: " + closeStr);
    endLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

    box.getChildren().addAll(startLbl, endLbl);
    return box;
  }

  /** Label hiển thị người đang dẫn đầu (chỉ khi RUNNING và đã có bid) */
  private Label buildLeaderLabel(Dto.AuctionCardDto session, String status) {
    Label label = new Label();
    if ("RUNNING".equals(status) && session.currentWinnerName() != null
        && !session.currentWinnerName().isBlank()) {
      label.setText("👑 Đang dẫn: " + session.currentWinnerName());
      label.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 12px; -fx-font-weight: bold;");
    } else {
      label.setText("👑 Chưa có ai đặt giá");
      label.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
    }
    return label;
  }

  /** Load ảnh sản phẩm với fallback */
  private void loadImage(javafx.scene.image.ImageView imgView, String imageUrl) {
    if (imageUrl == null || imageUrl.trim().isEmpty()) {
      setFallbackImage(imgView);
      return;
    }
    String url = imageUrl.trim();

    // Tự động thêm https:// nếu người dùng nhập thiếu
    if (!url.matches("^(?i)(http|https|file|data):.*") && !url.contains(":\\") && !url.contains(":/")) {
      java.io.File f = new java.io.File(url);
      if (!f.exists()) {
        url = "https://" + url;
      }
    }

    // Định dạng lại đường dẫn file cục bộ
    if (!url.matches("^(?i)(http|https|file|data):.*")) {
      java.io.File f = new java.io.File(url);
      if (f.exists()) {
        url = f.toURI().toString();
      } else {
        url = "file:/" + url.replace("\\", "/"); 
      }
    }

    try {
      javafx.scene.image.Image img = new javafx.scene.image.Image(url, true);
      img.errorProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal) {
          Platform.runLater(() -> setFallbackImage(imgView));
        }
      });
      imgView.setImage(img);
    } catch (Exception e) {
      setFallbackImage(imgView);
    }
  }

  private void setFallbackImage(javafx.scene.image.ImageView imgView) {
    try {
      java.io.InputStream resourceStream = getClass().getResourceAsStream("/img/logoo.jpg");
      if (resourceStream != null) {
        javafx.scene.image.Image fallback = new javafx.scene.image.Image(resourceStream);
        if (!fallback.isError() && fallback.getWidth() > 0) {
          imgView.setImage(fallback);
          return;
        }
      }
    } catch (Exception ignored) {
    }
    imgView.setImage(new javafx.scene.image.Image("https://placehold.co/220x160/png?text=No+Image", true));
  }

  private VBox createInfoCard(String message) {
    VBox card = new VBox(10);
    card.getStyleClass().add("card-container");
    card.setPadding(new Insets(20));
    card.setPrefWidth(400);
    Label messageLabel = new Label(message);
    messageLabel.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 14px;");
    card.getChildren().add(messageLabel);
    return card;
  }

  // -------------------------------------------------------
  // COUNTDOWN TIMER
  // -------------------------------------------------------

  private void startCountdownTimer() {
    stopCountdownTimer();
    countdownTimeline = new Timeline(
        new KeyFrame(Duration.seconds(1), event -> refreshCountdownLabels()));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  private void stopCountdownTimer() {
    if (countdownTimeline != null) {
      countdownTimeline.stop();
      countdownTimeline = null;
    }
  }

  private void refreshCountdownLabels() {
    for (Map.Entry<String, Label> entry : countdownLabelsBySessionId.entrySet()) {
      String sessionId = entry.getKey();
      Label label = entry.getValue();
      String status = statusBySessionId.getOrDefault(sessionId, "OPEN");
      LocalDateTime startTime = startTimeBySessionId.get(sessionId);
      LocalDateTime endTime = endTimeBySessionId.get(sessionId);

      refreshCountdownLabel(label, status, startTime, endTime);
    }
  }

  // -------------------------------------------------------
  // BID / NAVIGATION
  // -------------------------------------------------------

  @SuppressWarnings("unused")
  private void handleBidAction(Dto.AuctionCardDto session) {
    if (!SessionManager.getInstance().isLoggedIn()) {
      ClientMain.getMainController().switchContent("login.fxml");
      return;
    }

    Dto.UserProfileResponse currentUser = SessionManager.getInstance().getCurrentUser();
    if (currentUser == null || "ADMIN".equals(currentUser.role())) {
      showError("Không thể đặt giá", "Admin không thể đặt giá.");
      return;
    }

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Đặt giá");
    dialog.setHeaderText("Nhập giá cho sản phẩm: " + session.itemName());
    dialog.setContentText("Giá của bạn (VND):");

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(rawAmount -> submitBid(session.sessionId(), rawAmount));
  }

  private void submitBid(String sessionId, String rawAmount) {
    try {
      double amount = Double.parseDouble(rawAmount.replace(",", "").trim());
      if (amount <= 0) {
        showError("Giá không hợp lệ", "Giá đặt phải lớn hơn 0.");
        return;
      }
      Dto.PlaceBidRequest payload = new Dto.PlaceBidRequest(sessionId, amount);
      SocketClient.getInstance().sendRequest(new Request(ActionType.PLACE_BID, payload));
    } catch (NumberFormatException e) {
      showError("Giá không hợp lệ", "Vui lòng nhập số hợp lệ, ví dụ: 1250000");
    }
  }

  private void goToAuctionDetail(String sessionId) {
    try {
      System.out.println("--- Bắt đầu chuyển trang cho ID: " + sessionId + " ---");

      SessionManager.getInstance().setSelectedAuctionSessionId(sessionId);

      // Cập nhật menu sáng lên ở nút "Phòng đấu giá"
      ClientMain.getMainController().setActiveNavAuctionRoom();
      // Gọi hàm chuyển trang
      ClientMain.getMainController().switchContent("auction_detail.fxml");

      System.out.println("--- Chuyển trang thành công ---");
    } catch (Exception e) {
      System.err.println("!!! LỖI KHI BẤM NÚT CHI TIẾT:");
      e.printStackTrace();
    }
  }

  // -------------------------------------------------------
  // ALERTS
  // -------------------------------------------------------

  private void showInfo(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private void showError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  @Override
  public void onBeforeHide() {
    stopCountdownTimer();
    countdownLabelsBySessionId.clear();
    statusBySessionId.clear();
    startTimeBySessionId.clear();
    endTimeBySessionId.clear();
    SocketClient.getInstance().removeListener(responseListener);
  }

  public void scrollToAndFilterActiveAuctions() {
    currentFilter = "ALL";
    searchKeyword = "";
    if (searchInput != null) {
      searchInput.clear();
    }
    updateCategoryItemStyles(allCategoryItem);
    applyFilter();
    
    if (mainScrollPane != null) {
      Platform.runLater(() -> {
        mainScrollPane.setVvalue(1.0);
      });
    }
  }
}
