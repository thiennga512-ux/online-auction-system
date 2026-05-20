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
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * AuctionDetailController — Man hinh chi tiet phien dau gia.
 *
 * - OPEN    : dem nguoc den gio mo phien
 * - RUNNING : dem nguoc den gio ket thuc + co khu vuc dat gia
 * - FINISHED: hien thi ket qua, an bid panel
 */
public class AuctionDetailController implements LifecycleAwareController {

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
  @FXML private ImageView productImageView;
  @FXML private ListView<String> bidHistoryList;
  @FXML private Button    notifyButton;

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

  private Timeline countdownTimeline;
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
      showError("Thieu du lieu", "Khong tim thay phien dau gia can xem.");
      goBack();
      return;
    }
    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);

    Dto.SubscribeRequest sub = new Dto.SubscribeRequest(sessionId);
    SocketClient.getInstance().sendRequest(new Request(ActionType.SUBSCRIBE_AUCTION, sub));
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ACTIVE_AUCTIONS, null));
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_AUCTION_BIDS, sub));

    startCountdownTimer();
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
        titleLabel.setText("Khong tim thay phien hoac phien da ket thuc");
        countdownLabel.setText("Phien khong kha dung");
        countdownLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
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
        descriptionLabel.setText((d != null && !d.isBlank()) ? d : "Khong co mo ta san pham.");
      }

      priceLabel.setText("Gia hien tai: " + formatMoney(currentPrice) + " VND");
      sellerLabel.setText("Nguoi ban: " + selected.sellerName());

      if (selected.currentWinnerName() != null && !selected.currentWinnerName().isBlank()) {
        leaderLabel.setText("Dang dan dau: " + selected.currentWinnerName());
        leaderLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #fbbf24;");
      } else {
        leaderLabel.setText("Chua co nguoi dat gia");
        leaderLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
      }

      startTimeLabel.setText("Khai mac: " + currentStartTime.format(DT_FMT));
      endTimeLabel.setText("Ket thuc: " + currentEndTime.format(DT_FMT));
      applyStatusBadge(currentStatus);
      refreshCountdown();
      updateBidPanel();
      loadImage(selected.imageUrl());
    });
  }

  private void handleBidHistory(Response response) {
    if (!response.isSuccess()) {
      showError("Khong tai duoc lich su bid", response.getMessage());
      return;
    }
    List<Bid> bids = response.getDataAsList(Bid.class).stream()
        .filter(b -> sessionId.equals(b.getAuctionSessionId()))
        .sorted(Comparator.comparing(Bid::getTimestamp).reversed())
        .toList();
    Platform.runLater(() -> {
      bidHistoryList.getItems().clear();
      if (bids.isEmpty()) {
        bidHistoryList.getItems().add("Chua co luot dat gia nao.");
        return;
      }
      bids.forEach(bid -> bidHistoryList.getItems().add(formatBidEntry(bid)));
    });
  }

  private void handleNewBidEvent(Response response) {
    if (!response.isSuccess()) return;
    Dto.NewBidEvent event = response.getDataAs(Dto.NewBidEvent.class);
    if (event == null || !sessionId.equals(event.sessionId())) return;
    Platform.runLater(() -> {
      currentPrice = event.amount();
      priceLabel.setText("Gia hien tai: " + formatMoney(currentPrice) + " VND");
      leaderLabel.setText("Dang dan dau: " + event.bidderName());
      leaderLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #fbbf24;");
      bidHistoryList.getItems().add(0, formatBidEntry(event));
      updateBidPanel();
      if (isNotificationRegistered) {
        Dto.UserProfileResponse me = SessionManager.getInstance().getCurrentUser();
        if (me != null && !me.id().equals(event.bidderId())) {
          showInfo("Co bid moi!", event.bidderName() + " vua dat " + formatMoney(event.amount()) + " VND");
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
      showInfo("Phien da bat dau!", "Phien dau gia vua khai mac. Hay dat gia ngay!");
    });
  }

  private void handleAuctionEnded(Response response) {
    Platform.runLater(() -> {
      currentStatus = "FINISHED";
      stopCountdownTimer();
      countdownLabel.setText("Da ket thuc");
      countdownLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
      applyStatusBadge("FINISHED");
      updateBidPanel();
      showInfo("Phien dau gia da ket thuc", response.getMessage());
    });
  }

  private void handlePlaceBidResponse(Response response) {
    Platform.runLater(() -> {
      if (response.isSuccess()) {
        setBidStatus(true, "Dat gia thanh cong!");
        if (bidAmountField != null) bidAmountField.clear();
      } else {
        setBidStatus(false, "Loi: " + response.getMessage());
      }
    });
  }

  // -------------------------------------------------------
  // STATUS BADGE
  // -------------------------------------------------------

  private void applyStatusBadge(String status) {
    if (statusBadgeLabel == null) return;
    switch (status) {
      case "RUNNING" -> {
        statusBadgeLabel.setText("DANG DAU GIA");
        statusBadgeLabel.setStyle(
            "-fx-background-color: rgba(239,68,68,0.2);" +
            "-fx-border-color: #ef4444; -fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14;");
      }
      case "OPEN" -> {
        statusBadgeLabel.setText("SAP MO - CHO KHAI MAC");
        statusBadgeLabel.setStyle(
            "-fx-background-color: rgba(59,130,246,0.15);" +
            "-fx-border-color: #3b82f6; -fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-text-fill: #93c5fd; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14;");
      }
      default -> {
        statusBadgeLabel.setText("DA KET THUC");
        statusBadgeLabel.setStyle(
            "-fx-background-color: rgba(107,114,128,0.2);" +
            "-fx-border-color: #6b7280; -fx-border-radius: 20; -fx-background-radius: 20;" +
            "-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14;");
      }
    }
  }

  // -------------------------------------------------------
  // COUNTDOWN
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
        countdownLabel.setText("Da het gio");
        countdownLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
      } else {
        String color = TimeFormatUtil.getEndCountdownColor(currentEndTime);
        countdownLabel.setText("Ket thuc sau: " + TimeFormatUtil.formatRemainingTime(currentEndTime));
        countdownLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
      }
    } else if ("OPEN".equals(currentStatus)) {
      if (currentStartTime == null) return;
      if (LocalDateTime.now().isAfter(currentStartTime)) {
        countdownLabel.setText("Chuan bi khai mac...");
        countdownLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #60a5fa;");
      } else {
        String color = TimeFormatUtil.getOpenCountdownColor(currentStartTime);
        countdownLabel.setText("Mo sau: " + TimeFormatUtil.formatRemainingTime(currentStartTime));
        countdownLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
      }
    }
  }

  // -------------------------------------------------------
  // BID PANEL
  // -------------------------------------------------------

  /**
   * Cap nhat trang thai cua bid panel dua vao currentStatus va login.
   * RUNNING + Bidder -> enable; con lai -> disable.
   */
  private void updateBidPanel() {
    if (bidPanel == null) return;
    boolean isRunning  = "RUNNING".equals(currentStatus);
    boolean isLoggedIn = SessionManager.getInstance().isLoggedIn();
    Dto.UserProfileResponse me = SessionManager.getInstance().getCurrentUser();
    boolean isBidder   = isLoggedIn && me != null
                         && !"ADMIN".equals(me.role())
                         && !"SELLER".equals(me.role());
    boolean canBid = isRunning && isBidder;

    if (submitBidBtn != null) {
      submitBidBtn.setDisable(!canBid);
      submitBidBtn.setStyle(canBid
          ? "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-size: 14px;"
          : "-fx-background-color: #374151; -fx-text-fill: #6b7280; -fx-font-weight: bold; " +
            "-fx-background-radius: 8; -fx-padding: 10 20; -fx-font-size: 14px;");
    }
    if (bidAmountField != null) bidAmountField.setDisable(!canBid);
    setPresetDisable(!canBid);

    if (minBidHintLabel != null) {
      if (!isRunning) {
        minBidHintLabel.setText("Phien dang " + ("OPEN".equals(currentStatus) ? "cho mo" : "ket thuc"));
      } else if (!isBidder) {
        minBidHintLabel.setText("Dang nhap bang tai khoan Bidder de dat gia");
      } else {
        double nextMin = currentPrice + minIncrement;
        minBidHintLabel.setText("Gia toi thieu tiep theo: " + formatMoney(nextMin) + " VND");
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

  /** Them so tien vao o nhap. Click nhieu lan se cong chong. */
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
      setBidStatus(false, "Vui long dang nhap de dat gia.");
      return;
    }
    if (bidAmountField == null) return;
    String raw = bidAmountField.getText().replace(",", "").trim();
    double amount;
    try {
      amount = Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      setBidStatus(false, "Vui long nhap so hop le, vi du: 5000000");
      return;
    }
    if (amount <= currentPrice) {
      setBidStatus(false, "Gia phai lon hon " + formatMoney(currentPrice) + " VND");
      return;
    }
    Dto.PlaceBidRequest payload = new Dto.PlaceBidRequest(sessionId, amount);
    SocketClient.getInstance().sendRequest(new Request(ActionType.PLACE_BID, payload));
    setBidStatus(true, "Dang xu ly...");
  }

  private void setBidStatus(boolean ok, String msg) {
    if (bidStatusLabel == null) return;
    bidStatusLabel.setText(msg);
    bidStatusLabel.setStyle(ok
        ? "-fx-font-size: 12px; -fx-text-fill: #10b981;"
        : "-fx-font-size: 12px; -fx-text-fill: #ef4444;");
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
      showError("Chua dang nhap", "Vui long dang nhap de dang ky nhan thong bao.");
      return;
    }
    Dto.RegisterNotificationRequest payload = new Dto.RegisterNotificationRequest(sessionId);
    SocketClient.getInstance().sendRequest(new Request(ActionType.REGISTER_NOTIFICATION, payload));
  }

  private void handleRegisterNotificationResponse(Response response) {
    if (response.isSuccess()) {
      isNotificationRegistered = true;
      showInfo("Thanh cong", response.getMessage());
      Platform.runLater(() -> {
        if (notifyButton != null) {
          notifyButton.setText("Da dang ky");
          notifyButton.setDisable(true);
        }
      });
    } else {
      showError("Loi", response.getMessage());
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
  // IMAGE LOADING
  // -------------------------------------------------------

  private void loadImage(String imageUrl) {
    if (productImageView == null) return;
    if (imageUrl == null || imageUrl.trim().isEmpty()) {
      setDefaultImage("No Image");
      return;
    }

    String urlString = imageUrl.trim();
    if (!urlString.toLowerCase().startsWith("http")) urlString = "https://" + urlString;

    final String finalUrl = urlString;

    new Thread(() -> {
      HttpURLConnection connection = null;
      try {
        URL url = new URL(finalUrl);
        connection = (HttpURLConnection) url.openConnection();

        // Thiết lập các thuộc tính để "đánh lừa" server chặt chẽ hơn
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        connection.setRequestProperty("Referer", "https://google.com"); // Một số bên chặn nếu không có nguồn dẫn

        connection.setConnectTimeout(10000); // Đợi tối đa 10s
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true); // Tự động đi theo link chuyển hướng

        // Kiểm tra phản hồi từ Server
        int status = connection.getResponseCode();

        // Xử lý nếu server yêu cầu chuyển hướng thủ công (301, 302)
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM) {
          String newUrl = connection.getHeaderField("Location");
          loadImage(newUrl); // Gọi lại với link mới
          return;
        }

        try (InputStream is = connection.getInputStream()) {
          javafx.scene.image.Image img = new javafx.scene.image.Image(is);

          Platform.runLater(() -> {
            if (img.isError() || img.getWidth() <= 0) {
              setDefaultImage("Error Link");
            } else {
              productImageView.setImage(img);
            }
          });
        }
      } catch (Exception e) {
        System.err.println("Loi tai anh: " + e.getMessage());
        Platform.runLater(() -> setDefaultImage("Invalid URL"));
      } finally {
        if (connection != null) connection.disconnect();
      }
    }).start();
  }

  // Hàm phụ để set ảnh mặc định cho gọn code
  private void setDefaultImage(String text) {
    String placeholderUrl = "https://placehold.co/360x220/png?text=" + text.replace(" ", "+");
    productImageView.setImage(new javafx.scene.image.Image(placeholderUrl, true));
  }

  // -------------------------------------------------------
  // FORMATTING
  // -------------------------------------------------------

  private String formatBidEntry(Bid bid) {
    return String.format("%s  |  %s  |  %s VND  (%s)",
        bid.getTimestamp().format(DT_FMT),
        bid.getBidderName(),
        formatMoney(bid.getAmount()),
        bid.getBidType().getDisplayName());
  }

  private String formatBidEntry(Dto.NewBidEvent event) {
    return String.format("%s  |  %s  |  %s VND",
        event.timestamp(), event.bidderName(), formatMoney(event.amount()));
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
    SocketClient.getInstance().removeListener(listener);
    unsubscribeCurrentSession();
  }
}
