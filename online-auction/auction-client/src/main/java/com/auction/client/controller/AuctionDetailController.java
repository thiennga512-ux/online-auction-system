package com.auction.client.controller;

import com.auction.client.ClientMain;
import com.auction.client.network.SocketClient;
import com.auction.client.util.SessionManager;
import com.auction.client.util.TimeFormatUtil;
import com.auction.common.dto.Dto;
import com.auction.common.model.auction.Bid;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.common.network.Response;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * AuctionDetailController — Màn hình Phòng Đấu Giá Trực Tiếp.
 *
 * - OPEN    : đếm ngược đến giờ mở phiên
 * - RUNNING : đếm ngược đến giờ kết thúc + khu vực đặt giá
 * - FINISHED: hiển thị kết quả, ẩn bid panel
 */
public class AuctionDetailController implements LifecycleAwareController {

  private static final String EMPTY_BID_MSG = "Chưa có lượt đặt giá nào.";
  private static final double BID_ROW_HEIGHT = 52;

  // ===== FXML =====
  @FXML private Label     titleLabel;
  @FXML private Label     descriptionLabel;
  @FXML private Label     priceLabel;
  @FXML private Label     sellerLabel;
  @FXML private Label     leaderLabel;
  @FXML private Label     startTimeLabel;
  @FXML private Label     endTimeLabel;
  @FXML private Label     statusBadgeLabel;
  @FXML private Label     countdownLabel;
  @FXML private Label     viewerCountLabel;
  @FXML private ImageView productImageView;
  @FXML private ListView<String> bidHistoryList;
  @FXML private Button    notifyButton;
  @FXML private ProgressBar timeProgressBar;
  @FXML private Region    livePulseDot;

  // Bid panel
  @FXML private VBox      bidPanel;
  @FXML private Label     minBidHintLabel;
  @FXML private TextField bidAmountField;
  @FXML private Button    submitBidBtn;
  @FXML private Label     bidStatusLabel;
  @FXML private Button    preset1Btn;
  @FXML private Button    preset2Btn;
  @FXML private Button    preset3Btn;
  @FXML private Button    preset4Btn;
  @FXML private Button    preset5Btn;

  // ===== State =====
  private String        sessionId;
  private String        currentStatus;
  private LocalDateTime currentStartTime;
  private LocalDateTime currentEndTime;
  private double        currentPrice  = 0;
  private double        minIncrement  = 100_000;
  private boolean       isNotificationRegistered = false;
  private int           watcherEstimate = 24;

  private Timeline countdownTimeline;
  private Timeline pulseTimeline;
  private static final DateTimeFormatter DT_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  private final Consumer<Response> listener = this::handleResponse;

  // -------------------------------------------------------
  // INIT
  // -------------------------------------------------------

  @FXML
  public void initialize() {
    sessionId = SessionManager.getInstance().getSelectedAuctionSessionId();
    if (sessionId == null || sessionId.isBlank()) {
      showError("Thiếu dữ liệu", "Không tìm thấy phiên đấu giá cần xem.");
      goBack();
      return;
    }

    setupBidHistoryList();
    watcherEstimate = 40 + Math.abs(sessionId.hashCode() % 180);
    updateViewerCountLabel();

    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);

    Dto.SubscribeRequest sub = new Dto.SubscribeRequest(sessionId);
    SocketClient.getInstance().sendRequest(new Request(ActionType.SUBSCRIBE_AUCTION, sub));
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ACTIVE_AUCTIONS, null));
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_AUCTION_BIDS, sub));

    startCountdownTimer();
  }

  private void setupBidHistoryList() {
    if (bidHistoryList == null) return;

    bidHistoryList.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().remove("bid-history-leader");
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
          return;
        }
        setText(item);
        if (getIndex() == 0 && !EMPTY_BID_MSG.equals(item)) {
          getStyleClass().add("bid-history-leader");
        }
      }
    });

    bidHistoryList.getItems().addListener((ListChangeListener<String>) c -> updateBidListHeight());
    updateBidListHeight();
  }

  private void updateBidListHeight() {
    if (bidHistoryList == null) return;
    int count = bidHistoryList.getItems().size();
    bidHistoryList.setPrefHeight(Math.max(120, count * BID_ROW_HEIGHT + 12));
  }

  // -------------------------------------------------------
  // RESPONSE HANDLING
  // -------------------------------------------------------

  private void handleResponse(Response response) {
    if (response == null || response.getActionType() == null) return;
    switch (response.getActionType()) {
      case GET_ACTIVE_AUCTIONS       -> handleActiveAuctions(response);
      case GET_AUCTION_BIDS          -> handleBidHistory(response);
      case NEW_BID_BROADCAST         -> handleNewBidEvent(response);
      case AUCTION_STARTED_BROADCAST -> handleAuctionStarted(response);
      case AUCTION_ENDED_BROADCAST   -> handleAuctionEnded(response);
      case PLACE_BID                 -> handlePlaceBidResponse(response);
      case REGISTER_NOTIFICATION     -> handleRegisterNotificationResponse(response);
      default -> { }
    }
  }

  private void handleActiveAuctions(Response response) {
    if (!response.isSuccess()) return;
    List<Dto.AuctionCardDto> sessions = response.getDataAsList(Dto.AuctionCardDto.class);
    Dto.AuctionCardDto selected = sessions.stream()
        .filter(s -> sessionId.equals(s.sessionId()))
        .findFirst().orElse(null);

    if (selected == null) {
      Platform.runLater(() -> {
        titleLabel.setText("Không tìm thấy phiên hoặc phiên đã kết thúc");
        setCountdownStyle("muted");
        countdownLabel.setText("Phiên không khả dụng");
      });
      return;
    }

    Platform.runLater(() -> {
      currentStatus    = selected.status();
      currentStartTime = LocalDateTime.parse(selected.startTime());
      currentEndTime   = LocalDateTime.parse(selected.actualEndTime());
      currentPrice     = selected.currentPrice();
      minIncrement     = selected.minIncrement();

      titleLabel.setText(selected.itemName());

      if (descriptionLabel != null) {
        String d = selected.itemDescription();
        descriptionLabel.setText((d != null && !d.isBlank()) ? d : "Không có mô tả sản phẩm.");
      }

      updatePriceDisplay(currentPrice);
      sellerLabel.setText("👤 Người bán: " + selected.sellerName());

      if (selected.currentWinnerName() != null && !selected.currentWinnerName().isBlank()) {
        leaderLabel.setText("👑 Đang dẫn đầu: " + selected.currentWinnerName());
        leaderLabel.getStyleClass().remove("auction-leader-label-muted");
      } else {
        leaderLabel.setText("👑 Chưa có người đặt giá");
        if (!leaderLabel.getStyleClass().contains("auction-leader-label-muted")) {
          leaderLabel.getStyleClass().add("auction-leader-label-muted");
        }
      }

      startTimeLabel.setText("📅 Khai mạc: " + currentStartTime.format(DT_FMT));
      endTimeLabel.setText("🏁 Kết thúc: " + currentEndTime.format(DT_FMT));
      applyStatusBadge(currentStatus);
      refreshCountdown();
      updateBidPanel();
      loadImage(selected.imageUrl());
    });
  }

  private void handleBidHistory(Response response) {
    if (!response.isSuccess()) {
      showError("Không tải được lịch sử đặt giá", response.getMessage());
      return;
    }
    List<Bid> bids = response.getDataAsList(Bid.class).stream()
        .filter(b -> sessionId.equals(b.getAuctionSessionId()))
        .sorted(Comparator.comparing(Bid::getTimestamp).reversed())
        .toList();
    Platform.runLater(() -> {
      bidHistoryList.getItems().clear();
      if (bids.isEmpty()) {
        bidHistoryList.getItems().add(EMPTY_BID_MSG);
      } else {
        bids.forEach(bid -> bidHistoryList.getItems().add(formatBidEntry(bid)));
      }
      updateBidListHeight();
      updateViewerCount(bids);
    });
  }

  private void handleNewBidEvent(Response response) {
    if (!response.isSuccess()) return;
    Dto.NewBidEvent event = response.getDataAs(Dto.NewBidEvent.class);
    if (event == null || !sessionId.equals(event.sessionId())) return;
    Platform.runLater(() -> {
      currentPrice = event.amount();
      updatePriceDisplay(currentPrice);
      leaderLabel.setText("👑 Đang dẫn đầu: " + event.bidderName());
      leaderLabel.getStyleClass().remove("auction-leader-label-muted");

      if (bidHistoryList.getItems().size() == 1
          && EMPTY_BID_MSG.equals(bidHistoryList.getItems().get(0))) {
        bidHistoryList.getItems().clear();
      }
      bidHistoryList.getItems().add(0, formatBidEntry(event));
      updateBidListHeight();
      watcherEstimate = Math.min(999, watcherEstimate + 3);
      updateViewerCountLabel();
      updateBidPanel();

      if (isNotificationRegistered) {
        Dto.UserProfileResponse me = SessionManager.getInstance().getCurrentUser();
        if (me != null && !me.id().equals(event.bidderId())) {
          showInfo("Có bid mới!", event.bidderName() + " vừa đặt " + formatMoney(event.amount()) + " VND");
        }
      }
    });
  }

  private void handleAuctionStarted(Response response) {
    if (!response.isSuccess()) return;
    Dto.AuctionStartedEvent event = response.getDataAs(Dto.AuctionStartedEvent.class);
    if (event == null || !sessionId.equals(event.sessionId())) return;
    Platform.runLater(() -> {
      currentStatus = "RUNNING";
      applyStatusBadge("RUNNING");
      updateBidPanel();
      SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ACTIVE_AUCTIONS, null));
      showInfo("Phiên đã bắt đầu!", "Phiên đấu giá vừa khai mạc. Hãy đặt giá ngay!");
    });
  }

  private void handleAuctionEnded(Response response) {
    Platform.runLater(() -> {
      currentStatus = "FINISHED";
      stopCountdownTimer();
      stopLivePulse();
      setCountdownStyle("muted");
      countdownLabel.setText("Đã kết thúc");
      if (timeProgressBar != null) {
        timeProgressBar.setProgress(0);
      }
      applyStatusBadge("FINISHED");
      updateBidPanel();
      showInfo("Phiên đấu giá đã kết thúc", response.getMessage());
    });
  }

  private void handlePlaceBidResponse(Response response) {
    Platform.runLater(() -> {
      if (response.isSuccess()) {
        setBidStatus(true, "Đặt giá thành công!");
        if (bidAmountField != null) bidAmountField.clear();
      } else {
        setBidStatus(false, "Lỗi: " + response.getMessage());
      }
    });
  }

  // -------------------------------------------------------
  // STATUS BADGE & LIVE PULSE
  // -------------------------------------------------------

  private void applyStatusBadge(String status) {
    if (statusBadgeLabel == null) return;
    statusBadgeLabel.getStyleClass().removeAll(
        "status-running", "status-open", "status-finished");

    switch (status) {
      case "RUNNING" -> {
        statusBadgeLabel.setText("ĐANG DIỄN RA");
        statusBadgeLabel.getStyleClass().add("status-running");
        statusBadgeLabel.setStyle(
            "-fx-background-color: rgba(239,68,68,0.2);" +
            "-fx-border-color: #ef4444; -fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-text-fill: #fca5a5; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 14;");
        startLivePulse();
      }
      case "OPEN" -> {
        statusBadgeLabel.setText("SẮP MỞ — CHỜ KHAI MẠC");
        statusBadgeLabel.getStyleClass().add("status-open");
        statusBadgeLabel.setStyle(
            "-fx-background-color: rgba(59,130,246,0.15);" +
            "-fx-border-color: #3b82f6; -fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-text-fill: #93c5fd; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 14;");
        stopLivePulse();
      }
      default -> {
        statusBadgeLabel.setText("ĐÃ KẾT THÚC");
        statusBadgeLabel.getStyleClass().add("status-finished");
        statusBadgeLabel.setStyle(
            "-fx-background-color: rgba(107,114,128,0.2);" +
            "-fx-border-color: #6b7280; -fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 14;");
        stopLivePulse();
      }
    }
  }

  private void startLivePulse() {
    if (livePulseDot == null) return;
    livePulseDot.setVisible(true);
    livePulseDot.setManaged(true);
    stopLivePulse();
    pulseTimeline = new Timeline(
        new KeyFrame(Duration.ZERO, e -> livePulseDot.setOpacity(1.0)),
        new KeyFrame(Duration.millis(550), e -> livePulseDot.setOpacity(0.25)),
        new KeyFrame(Duration.millis(1100), e -> livePulseDot.setOpacity(1.0))
    );
    pulseTimeline.setCycleCount(Timeline.INDEFINITE);
    pulseTimeline.play();
  }

  private void stopLivePulse() {
    if (pulseTimeline != null) {
      pulseTimeline.stop();
      pulseTimeline = null;
    }
    if (livePulseDot != null) {
      livePulseDot.setVisible(false);
      livePulseDot.setManaged(false);
      livePulseDot.setOpacity(1.0);
    }
  }

  // -------------------------------------------------------
  // COUNTDOWN & PROGRESS
  // -------------------------------------------------------

  private void startCountdownTimer() {
    stopCountdownTimer();
    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshCountdown()));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  private void stopCountdownTimer() {
    if (countdownTimeline != null) {
      countdownTimeline.stop();
      countdownTimeline = null;
    }
  }

  private void refreshCountdown() {
    if (countdownLabel == null) return;
    if ("RUNNING".equals(currentStatus)) {
      if (currentEndTime == null) return;
      if (LocalDateTime.now().isAfter(currentEndTime)) {
        setCountdownStyle("muted");
        countdownLabel.setText("Đã hết giờ");
      } else {
        long secsLeft = java.time.Duration.between(LocalDateTime.now(), currentEndTime).getSeconds();
        boolean urgent = secsLeft < 300;
        setCountdownStyle(urgent ? "urgent" : "normal");
        countdownLabel.setText("Kết thúc sau: " + TimeFormatUtil.formatRemainingTime(currentEndTime));
      }
    } else if ("OPEN".equals(currentStatus)) {
      if (currentStartTime == null) return;
      if (LocalDateTime.now().isAfter(currentStartTime)) {
        setCountdownStyle("normal");
        countdownLabel.setText("Chuẩn bị khai mạc...");
      } else {
        setCountdownStyle("normal");
        countdownLabel.setText("Mở sau: " + TimeFormatUtil.formatRemainingTime(currentStartTime));
      }
    }
    updateTimeProgress();
  }

  private void setCountdownStyle(String mode) {
    if (countdownLabel == null) return;
    countdownLabel.getStyleClass().removeAll(
        "auction-countdown-muted", "auction-countdown-urgent");
    switch (mode) {
      case "muted" -> countdownLabel.getStyleClass().add("auction-countdown-muted");
      case "urgent" -> countdownLabel.getStyleClass().add("auction-countdown-urgent");
      default -> { }
    }
  }

  private void updateTimeProgress() {
    if (timeProgressBar == null) return;

    LocalDateTime now = LocalDateTime.now();
    double progress = 0;
    boolean urgent = false;

    if ("RUNNING".equals(currentStatus) && currentStartTime != null && currentEndTime != null) {
      long total = Math.max(1, java.time.Duration.between(currentStartTime, currentEndTime).getSeconds());
      long remaining = Math.max(0, java.time.Duration.between(now, currentEndTime).getSeconds());
      progress = Math.min(1.0, (double) remaining / total);
      urgent = progress < 0.15;
    } else if ("OPEN".equals(currentStatus) && currentStartTime != null) {
      long untilStart = Math.max(0, java.time.Duration.between(now, currentStartTime).getSeconds());
      long window = Math.max(untilStart, 3600);
      progress = Math.min(1.0, (double) untilStart / window);
      urgent = untilStart < 600;
    } else if ("FINISHED".equals(currentStatus)) {
      progress = 0;
    } else {
      progress = 1.0;
    }

    timeProgressBar.setProgress(progress);
    if (urgent) {
      if (!timeProgressBar.getStyleClass().contains("auction-time-progress-urgent")) {
        timeProgressBar.getStyleClass().add("auction-time-progress-urgent");
      }
    } else {
      timeProgressBar.getStyleClass().remove("auction-time-progress-urgent");
    }
  }

  // -------------------------------------------------------
  // VIEWER COUNT
  // -------------------------------------------------------

  private void updateViewerCount(List<Bid> bids) {
    Set<String> bidders = new HashSet<>();
    bids.forEach(b -> {
      if (b.getBidderName() != null) bidders.add(b.getBidderName());
    });
    watcherEstimate = Math.max(watcherEstimate, 20 + bidders.size() * 8 + bids.size() * 2);
    updateViewerCountLabel();
  }

  private void updateViewerCountLabel() {
    if (viewerCountLabel == null) return;
    viewerCountLabel.setText(watcherEstimate + " người đang xem");
  }

  // -------------------------------------------------------
  // BID PANEL
  // -------------------------------------------------------

  private void updateBidPanel() {
    if (bidPanel == null) return;
    boolean isRunning  = "RUNNING".equals(currentStatus);
    boolean isLoggedIn = SessionManager.getInstance().isLoggedIn();
    Dto.UserProfileResponse me = SessionManager.getInstance().getCurrentUser();
    boolean isBidder   = isLoggedIn && me != null
                         && !"ADMIN".equals(me.role())
                         && !"SELLER".equals(me.role());
    boolean canBid = isRunning && isBidder;

    if (submitBidBtn != null) submitBidBtn.setDisable(!canBid);
    if (bidAmountField != null) bidAmountField.setDisable(!canBid);
    setPresetDisable(!canBid);

    if (minBidHintLabel != null) {
      if (!isRunning) {
        minBidHintLabel.setText("Phiên đang " + ("OPEN".equals(currentStatus) ? "chờ mở" : "kết thúc"));
      } else if (!isBidder) {
        minBidHintLabel.setText("Đăng nhập bằng tài khoản Bidder để đặt giá");
      } else {
        double nextMin = currentPrice + minIncrement;
        minBidHintLabel.setText("Giá tối thiểu tiếp theo: " + formatMoney(nextMin) + " VND");
      }
    }
    if (bidStatusLabel != null) bidStatusLabel.setText("");
  }

  private void setPresetDisable(boolean disabled) {
    if (preset1Btn != null) preset1Btn.setDisable(disabled);
    if (preset2Btn != null) preset2Btn.setDisable(disabled);
    if (preset3Btn != null) preset3Btn.setDisable(disabled);
    if (preset4Btn != null) preset4Btn.setDisable(disabled);
    if (preset5Btn != null) preset5Btn.setDisable(disabled);
  }

  private void addPreset(double amount) {
    if (bidAmountField == null) return;
    String cur = bidAmountField.getText().replace(",", "").trim();
    double base = 0;
    try { base = Double.parseDouble(cur); } catch (NumberFormatException ignored) {}
    double next = (base > 0 ? base : currentPrice) + amount;
    bidAmountField.setText(String.format("%.0f", next));
    if (bidStatusLabel != null) bidStatusLabel.setText("");
  }

  @FXML private void handlePreset1() { addPreset(100_000); }
  @FXML private void handlePreset2() { addPreset(500_000); }
  @FXML private void handlePreset3() { addPreset(1_000_000); }
  @FXML private void handlePreset4() { addPreset(5_000_000); }
  @FXML private void handlePreset5() { addPreset(10_000_000); }

  @FXML
  private void handleSubmitBid() {
    if (!SessionManager.getInstance().isLoggedIn()) {
      setBidStatus(false, "Vui lòng đăng nhập để đặt giá.");
      return;
    }
    if (bidAmountField == null) return;
    String raw = bidAmountField.getText().replace(",", "").trim();
    double amount;
    try {
      amount = Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      setBidStatus(false, "Vui lòng nhập số hợp lệ, ví dụ: 5000000");
      return;
    }
    if (amount <= currentPrice) {
      setBidStatus(false, "Giá phải lớn hơn " + formatMoney(currentPrice) + " VND");
      return;
    }
    Dto.PlaceBidRequest payload = new Dto.PlaceBidRequest(sessionId, amount);
    SocketClient.getInstance().sendRequest(new Request(ActionType.PLACE_BID, payload));
    setBidStatus(true, "Đang xử lý...");
  }

  private void setBidStatus(boolean ok, String msg) {
    if (bidStatusLabel == null) return;
    bidStatusLabel.setText(msg);
    bidStatusLabel.getStyleClass().remove("auction-bid-status-ok");
    if (ok) {
      bidStatusLabel.getStyleClass().add("auction-bid-status-ok");
    }
  }

  private void updatePriceDisplay(double price) {
    if (priceLabel != null) {
      priceLabel.setText(formatMoney(price) + " VND");
    }
  }

  // -------------------------------------------------------
  // NAVIGATION & NOTIFICATIONS
  // -------------------------------------------------------

  @FXML
  private void handleRefresh() {
    Dto.SubscribeRequest sub = new Dto.SubscribeRequest(sessionId);
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ACTIVE_AUCTIONS, null));
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_AUCTION_BIDS, sub));
  }

  @FXML
  private void handleRegisterNotification() {
    if (!SessionManager.getInstance().isLoggedIn()) {
      showError("Chưa đăng nhập", "Vui lòng đăng nhập để đăng ký nhận thông báo.");
      return;
    }
    Dto.RegisterNotificationRequest payload = new Dto.RegisterNotificationRequest(sessionId);
    SocketClient.getInstance().sendRequest(new Request(ActionType.REGISTER_NOTIFICATION, payload));
  }

  private void handleRegisterNotificationResponse(Response response) {
    if (response.isSuccess()) {
      isNotificationRegistered = true;
      showInfo("Thành công", response.getMessage());
      Platform.runLater(() -> {
        if (notifyButton != null) {
          notifyButton.setText("✓ Đã đăng ký");
          notifyButton.setDisable(true);
        }
      });
    } else {
      showError("Lỗi", response.getMessage());
    }
  }

  @FXML
  private void handleBack() {
    unsubscribeCurrentSession();
    goBack();
  }

  private void unsubscribeCurrentSession() {
    if (sessionId == null || sessionId.isBlank()) return;
    Dto.SubscribeRequest payload = new Dto.SubscribeRequest(sessionId);
    SocketClient.getInstance().sendRequest(new Request(ActionType.UNSUBSCRIBE_AUCTION, payload));
  }

  private void goBack() {
    ClientMain.getMainController().switchContent("home.fxml");
  }

  // -------------------------------------------------------
  // IMAGE LOADING — Robust with Local File Support & Fallback
  // -------------------------------------------------------

  private void loadImage(String imageUrl) {
    if (productImageView == null) return;

    // 1. Validate input
    if (imageUrl == null || imageUrl.trim().isEmpty()) {
      setFallbackImage("Không+cos+ảnh");
      return;
    }

    String urlString = imageUrl.trim();

    // 2. Detect if it's a local file path (no protocol OR file:// protocol)
    //    Seller stores image as file.toURI().toString() → "file:///C:/path/to/img.jpg"
    //    Only http:// and https:// are treated as remote URLs.
    boolean isLocalPath = !urlString.startsWith("http://") && !urlString.startsWith("https://");

    if (isLocalPath) {
      // Local file: load synchronously on JavaFX thread (Image constructor supports file://)
      try {
        java.io.File file;
        if (urlString.startsWith("file:/")) {
          // Already a file:// URI — use directly
          javafx.scene.image.Image localImg = new javafx.scene.image.Image(urlString);
          if (!localImg.isError() && localImg.getWidth() > 0) {
            final javafx.scene.image.Image finalImg = localImg;
            Platform.runLater(() -> productImageView.setImage(finalImg));
            return;
          }
        } else {
          // Plain file path (e.g. C:\path\to\img.jpg or /home/user/img.jpg)
          file = new java.io.File(urlString);
          if (file.exists()) {
            javafx.scene.image.Image localImg = new javafx.scene.image.Image(file.toURI().toString());
            if (!localImg.isError() && localImg.getWidth() > 0) {
              final javafx.scene.image.Image finalImg = localImg;
              Platform.runLater(() -> productImageView.setImage(finalImg));
              return;
            }
          }
        }
      } catch (Exception e) {
        // Fall through to fallback
      }
      // If we reach here, local loading failed
      Platform.runLater(() -> setFallbackImage("Không+cos+ảnh"));
      return;
    }

    // 3. Remote URL: load asynchronously
    final String finalUrl = urlString;
    new Thread(() -> {
      HttpURLConnection connection = null;
      try {
        URL url = new URL(finalUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setRequestProperty("Accept", "image/*,*/*;q=0.8");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);

        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP
            || status == HttpURLConnection.HTTP_MOVED_PERM) {
          String newUrl = connection.getHeaderField("Location");
          loadImage(newUrl);
          return;
        }

        try (InputStream is = connection.getInputStream()) {
          javafx.scene.image.Image remoteImg = new javafx.scene.image.Image(is);
          Platform.runLater(() -> {
            if (remoteImg.isError() || remoteImg.getWidth() <= 0) {
              setFallbackImage("Lỗi+ảnh");
            } else {
              productImageView.setImage(remoteImg);
            }
          });
        }
      } catch (Exception e) {
        Platform.runLater(() -> setFallbackImage("Lỗi+tải+ảnh"));
      } finally {
        if (connection != null) connection.disconnect();
      }
    }).start();
  }

  /** Fallback: sử dụng placeholder mặc định từ resources để tránh ảnh đen */
  private void setFallbackImage(String fallbackText) {
    // Try loading from classpath resource first
    try {
      InputStream resourceStream = getClass().getResourceAsStream("/images/no-image.png");
      if (resourceStream != null) {
        javafx.scene.image.Image fallback = new javafx.scene.image.Image(resourceStream);
        if (!fallback.isError() && fallback.getWidth() > 0) {
          productImageView.setImage(fallback);
          return;
        }
      }
    } catch (Exception ignored) {
      // Fall through to online fallback
    }
    // Online fallback (vẫn an toàn, không bị đen)
    String placeholderUrl = "https://placehold.co/400x280/1a1f2b/9ca3af/png?text=" + fallbackText;
    productImageView.setImage(new javafx.scene.image.Image(placeholderUrl, true));
  }

  // -------------------------------------------------------
  // FORMATTING
  // -------------------------------------------------------

  private String formatBidEntry(Bid bid) {
    return String.format("🏆 %s  ·  %s  ·  %s VND  (%s)",
        bid.getTimestamp().format(DT_FMT),
        bid.getBidderName(),
        formatMoney(bid.getAmount()),
        bid.getBidType().getDisplayName());
  }

  private String formatBidEntry(Dto.NewBidEvent event) {
    String formattedTime;
    try {
      if (event.timestamp() != null && !event.timestamp().isBlank()) {
        // Parse ISO string (e.g. "2026-05-24T15:00:17.954891100") to LocalDateTime
        LocalDateTime ts = LocalDateTime.parse(event.timestamp());
        formattedTime = ts.format(DT_FMT);
      } else {
        formattedTime = "N/A";
      }
    } catch (Exception e) {
      formattedTime = "N/A";
    }
    return String.format("🏆 %s  ·  %s  ·  %s VND",
        formattedTime, event.bidderName(), formatMoney(event.amount()));
  }

  private String formatMoney(double value) {
    return String.format("%,.0f", value);
  }

  // -------------------------------------------------------
  // ALERTS
  // -------------------------------------------------------

  private void showInfo(String title, String message) {
    Platform.runLater(() -> {
      Alert a = new Alert(Alert.AlertType.INFORMATION);
      a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    });
  }

  private void showError(String title, String message) {
    Platform.runLater(() -> {
      Alert a = new Alert(Alert.AlertType.ERROR);
      a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    });
  }

  // -------------------------------------------------------
  // LIFECYCLE
  // -------------------------------------------------------

  @Override
  public void onBeforeHide() {
    stopCountdownTimer();
    stopLivePulse();
    SocketClient.getInstance().removeListener(listener);
    unsubscribeCurrentSession();
  }
}
