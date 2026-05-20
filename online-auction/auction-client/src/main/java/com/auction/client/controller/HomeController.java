package com.auction.client.controller;

import com.auction.client.ClientMain;
import com.auction.client.network.SocketClient;
import com.auction.client.util.SessionManager;
import com.auction.client.util.TimeFormatUtil;
import com.auction.common.dto.Dto;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class HomeController implements LifecycleAwareController {

  @FXML
  private FlowPane productsGrid;

  private final Consumer<com.auction.common.network.Response> responseListener = this::handleResponse;

  // Lưu label đếm ngược theo sessionId
  private final Map<String, Label>         countdownLabelsBySessionId = new HashMap<>();
  // Lưu trạng thái (OPEN / RUNNING) theo sessionId
  private final Map<String, String>        statusBySessionId          = new HashMap<>();
  // Lưu startTime (dùng khi OPEN)
  private final Map<String, LocalDateTime> startTimeBySessionId       = new HashMap<>();
  // Lưu actualEndTime (dùng khi RUNNING)
  private final Map<String, LocalDateTime> endTimeBySessionId         = new HashMap<>();

  private Timeline countdownTimeline;

  @FXML
  public void initialize() {
    SocketClient.getInstance().removeListener(responseListener);
    SocketClient.getInstance().addListener(responseListener);
    renderLoadingState();
    refreshActiveAuctions();
  }

  private void refreshActiveAuctions() {
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ACTIVE_AUCTIONS, null));
  }

  private void handleResponse(com.auction.common.network.Response response) {
    if (response.getActionType() == ActionType.GET_ACTIVE_AUCTIONS) {
      if (!response.isSuccess()) {
        showError("Không thể tải danh sách phiên đấu giá", response.getMessage());
        return;
      }
      List<Dto.AuctionCardDto> sessions = response.getDataAsList(Dto.AuctionCardDto.class);
      renderAuctions(sessions);
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
          System.err.println("[HomeController] Lỗi hiển thị thẻ đấu giá " + session.sessionId() + ": " + e.getMessage());
          e.printStackTrace();
        }
      }
      startCountdownTimer();
    });
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

    // ===== BADGE TRẠNG THÁI =====
    String status = session.status();
    Label statusBadge = createStatusBadge(status);

    // ===== TÊN SẢN PHẨM =====
    Label nameLabel = new Label(session.itemName());
    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
    nameLabel.setWrapText(true);

    // ===== MÔ TẢ SẢN PHẨM (tóm tắt) =====
    String rawDesc = session.itemDescription();
    String shortDesc = (rawDesc != null && !rawDesc.isBlank())
        ? (rawDesc.length() > 90 ? rawDesc.substring(0, 90).trim() + "…" : rawDesc)
        : "Không có mô tả";
    Label descLabel = new Label(shortDesc);
    descLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
    descLabel.setWrapText(true);

    // ===== GIÁ HIỆN TẠI =====
    String formattedPrice = String.format("%,.0f", session.currentPrice());
    Label priceLabel = new Label("Giá hiện tại: " + formattedPrice + " VND");
    priceLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 14px;");

    // ===== COUNTDOWN LABEL (thay đổi tuỳ status) =====
    LocalDateTime startTime = LocalDateTime.parse(session.startTime());
    LocalDateTime endTime   = LocalDateTime.parse(session.actualEndTime());

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

    // ===== THỜI GIAN MỞ / KẾT THÚC (dòng phụ, nhỏ) =====
    Label scheduleLabel = buildScheduleLabel(status, startTime, endTime);

    // ===== NÚT HÀNH ĐỘNG =====
    Button detailBtn = new Button("📋 CHI TIẾT");
    detailBtn.setMaxWidth(Double.MAX_VALUE);
    detailBtn.setOnAction(e -> goToAuctionDetail(session.sessionId()));

    Button bidBtn = new Button("🏷 ĐẶT GIÁ");
    bidBtn.getStyleClass().add("primary-button");
    bidBtn.setMaxWidth(Double.MAX_VALUE);
    bidBtn.setOnAction(e -> handleBidAction(session));

    // Khi OPEN (chưa bắt đầu) → disable nút đặt giá
    boolean isRunning = "RUNNING".equals(status);
    bidBtn.setDisable(!isRunning);
    if (!isRunning) {
      bidBtn.setText("⏳ CHƯA MỞ");
      bidBtn.setStyle("-fx-opacity: 0.5;");
    }

    HBox actionRow = new HBox(8, detailBtn, bidBtn);
    HBox.setHgrow(detailBtn, Priority.ALWAYS);
    HBox.setHgrow(bidBtn, Priority.ALWAYS);

    card.getChildren().addAll(imgView, statusBadge, nameLabel, descLabel, priceLabel,
        countdownLabel, scheduleLabel, sellerLabel, leaderLabel, actionRow);
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
          "-fx-padding: 4 10;"
      );
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
          "-fx-padding: 4 10;"
      );
    }
    return badge;
  }

  /**
   * Tạo Label đếm ngược chính.
   * - OPEN    → "⏳ Mở sau: X giờ Y phút Z giây"  (xanh lam)
   * - RUNNING → "⏱ Kết thúc sau: X phút Y giây"   (xanh ngọc / vàng / đỏ)
   */
  private Label buildCountdownLabel(String status, LocalDateTime startTime, LocalDateTime endTime) {
    Label label = new Label();
    label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
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
        label.setText("⏱ Kết thúc sau: " + TimeFormatUtil.formatRemainingTime(endTime));
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
      }
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

  /**
   * Dòng phụ nhỏ hiển thị lịch cụ thể:
   * - OPEN    → "Khai mạc: dd/MM HH:mm  |  Kết thúc: dd/MM HH:mm"
   * - RUNNING → "Bắt đầu: dd/MM HH:mm  |  Kết thúc: dd/MM HH:mm"
   */
  private Label buildScheduleLabel(String status, LocalDateTime startTime, LocalDateTime endTime) {
    java.time.format.DateTimeFormatter fmt =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm");
    String openStr  = startTime.format(fmt);
    String closeStr = endTime.format(fmt);

    String text;
    if ("RUNNING".equals(status)) {
      text = "🕐 Bắt đầu: " + openStr + "  |  Kết thúc: " + closeStr;
    } else {
      text = "📅 Khai mạc: " + openStr + "  |  Kết thúc: " + closeStr;
    }
    Label lbl = new Label(text);
    lbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
    lbl.setWrapText(true);
    return lbl;
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
    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
      String url = imageUrl.trim();
      if (!url.matches("^(?i)(http|https|file|data):.*")) {
        java.io.File f = new java.io.File(url);
        if (f.exists()) {
          url = f.toURI().toString();
        } else if (!url.contains(":/") && !url.contains(":\\")) {
          url = "https://" + url;
        } else {
          url = f.toURI().toString();
        }
      }
      try {
        javafx.scene.image.Image img = new javafx.scene.image.Image(url, true);
        img.errorProperty().addListener((obs, oldVal, newVal) -> {
          if (newVal) {
            imgView.setImage(new javafx.scene.image.Image(
                "https://placehold.co/220x160/png?text=No+Image", true));
          }
        });
        imgView.setImage(img);
      } catch (Exception e) {
        imgView.setImage(new javafx.scene.image.Image(
            "https://placehold.co/220x160/png?text=Error", true));
      }
    } else {
      imgView.setImage(new javafx.scene.image.Image(
          "https://placehold.co/220x160/png?text=No+Image", true));
    }
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
      Label label      = entry.getValue();
      String status    = statusBySessionId.getOrDefault(sessionId, "OPEN");
      LocalDateTime startTime = startTimeBySessionId.get(sessionId);
      LocalDateTime endTime   = endTimeBySessionId.get(sessionId);

      refreshCountdownLabel(label, status, startTime, endTime);
    }
  }

  // -------------------------------------------------------
  // BID / NAVIGATION
  // -------------------------------------------------------

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
    SessionManager.getInstance().setSelectedAuctionSessionId(sessionId);
    ClientMain.getMainController().switchContent("auction_detail.fxml");
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
}
