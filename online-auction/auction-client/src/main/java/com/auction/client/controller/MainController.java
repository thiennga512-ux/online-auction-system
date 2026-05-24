package com.auction.client.controller;

import com.auction.client.ClientMain;
import com.auction.client.network.SocketClient;
import com.auction.client.util.SessionManager;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Duration;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.auction.common.dto.Dto;

public class MainController {

  @FXML private HBox guestBox;
  @FXML private HBox userBox;
  @FXML private Label greetingLabel;
  @FXML private Label balanceLabel;
  @FXML private Button upgradeSellerButton;
  @FXML private Button depositButton;
  @FXML private Button sellerDashboardButton;
  @FXML private Button adminDashboardButton;

  @FXML private Button homeNavButton;
  @FXML private Button auctionRoomButton;
  @FXML private Button auctionHistoryButton;
  @FXML private Label timeLabel;
  @FXML private Label dateLabel;

  @FXML private StackPane contentArea;

  @FXML private Button notificationBellButton;
  @FXML private Label notificationBadge;
  
  private ContextMenu notificationMenu = new ContextMenu();
  private int unreadCount = 0;

  private Timeline clockTimeline;
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private Object activeContentController;

  @FXML
  public void initialize() {
    ClientMain.setMainController(this);
    setupClock();
    setActiveMenu(homeNavButton);

    // Lắng nghe sự kiện đăng nhập/đăng xuất để đổi Header
    SessionManager.getInstance().addLoginStateListener(user -> {
      Platform.runLater(() -> {
        if (user == null) {
          // Chưa đăng nhập
          guestBox.setVisible(true);
          guestBox.setManaged(true);
          userBox.setVisible(false);
          userBox.setManaged(false);
        } else {
          // Đã đăng nhập
          guestBox.setVisible(false);
          guestBox.setManaged(false);
          userBox.setVisible(true);
          userBox.setManaged(true);
          greetingLabel.setText("Xin chào, " + user.fullName());

          boolean isBidder = "BIDDER".equals(user.role());
          boolean isSeller = "SELLER".equals(user.role());
          boolean isAdmin  = "ADMIN".equals(user.role());

          // --- Nút nâng cấp Seller ---
          upgradeSellerButton.setVisible(isBidder);
          upgradeSellerButton.setManaged(isBidder);

          // --- Nút Nạp Tiền (chỉ Bidder) ---
          depositButton.setVisible(isBidder);
          depositButton.setManaged(isBidder);

          // --- Số dư (Bidder + Seller đều thấy) ---
          if (isBidder || isSeller) {
            balanceLabel.setText(formatVND(user.balance()));
            balanceLabel.setVisible(true);
            balanceLabel.setManaged(true);
          } else {
            balanceLabel.setVisible(false);
            balanceLabel.setManaged(false);
          }

          // --- Dashboard buttons ---
          sellerDashboardButton.setVisible(isSeller);
          sellerDashboardButton.setManaged(isSeller);
          adminDashboardButton.setVisible(isAdmin);
          adminDashboardButton.setManaged(isAdmin);
        }
      });
    });

    goToHome();

    // Lắng nghe response từ Server (Logout + Notification)
    SocketClient.getInstance().addListener(response -> {
      if (response.getActionType() == ActionType.LOGOUT && response.isSuccess()) {
        Platform.runLater(() -> {
          SessionManager.getInstance().setCurrentUser(null);
          goToHome();
        });
      } else if (response.getActionType() == ActionType.GLOBAL_NOTIFICATION_BROADCAST && response.isSuccess()) {
        Platform.runLater(() -> {
          Dto.NotificationEvent event = response.getDataAs(Dto.NotificationEvent.class);
          if (event != null) {
            unreadCount++;
            notificationBadge.setText(String.valueOf(unreadCount));
            notificationBadge.setVisible(true);
            notificationMenu.getItems().removeIf(item -> item.getText().equals("Không có thông báo mới"));
            MenuItem item = new MenuItem(event.message());
            notificationMenu.getItems().add(0, item);
          }
        });
      } else if ((response.getActionType() == ActionType.SELF_DEPOSIT || response.getActionType() == ActionType.SYNC_BALANCE) && response.isSuccess()) {
        // Cập nhật balance label trên header khi có thay đổi số dư
        Platform.runLater(() -> {
          Dto.DepositResultResponse result = response.getDataAs(Dto.DepositResultResponse.class);
          if (result != null) {
            balanceLabel.setText(formatVND(result.newBalance()));
            SessionManager.getInstance().updateBalance(result.newBalance());
          }
        });
      }
    });
  }

  public void switchContent(String fxmlFile) {
    try {
      String fxmlPath = "/fxml/" + fxmlFile;
      InputStream fxmlStream = getClass().getResourceAsStream(fxmlPath);
      if (fxmlStream == null) {
        throw new IllegalStateException("Không tìm thấy file FXML trên classpath: " + fxmlPath);
      }

      FXMLLoader loader = new FXMLLoader();
      // Load qua InputStream — tránh lỗi URL khi đường dẫn project có dấu/khoảng trắng
      try (InputStream in = fxmlStream) {
        Node node = loader.load(in);
        Object newController = loader.getController();

        if (activeContentController instanceof LifecycleAwareController lifecycleAware) {
          lifecycleAware.onBeforeHide();
        }

        activeContentController = newController;
        applyGlobalStylesheet(node);
        if (node instanceof Region region) {
          region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        contentArea.getChildren().setAll(node);
      }
    } catch (Exception e) {
      System.err.println("===== LOAD ERROR (" + fxmlFile + ") =====");
      e.printStackTrace();
      Platform.runLater(() -> {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Không mở được trang");
        alert.setHeaderText(fxmlFile);
        alert.setContentText(describeLoadError(e));
        alert.showAndWait();
      });
    }
  }

  /** Gắn CSS toàn cục qua classpath (không dùng đường dẫn tương đối @../css trong FXML). */
  private void applyGlobalStylesheet(Node node) {
    var cssUrl = getClass().getResource("/css/style.css");
    if (cssUrl == null || !(node instanceof Parent parent)) {
      return;
    }
    String css = cssUrl.toExternalForm();
    if (!parent.getStylesheets().contains(css)) {
      parent.getStylesheets().add(css);
    }
  }

  private static String describeLoadError(Throwable e) {
    StringBuilder sb = new StringBuilder();
    Throwable cur = e;
    while (cur != null) {
      if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
        if (sb.length() > 0) sb.append("\n→ ");
        sb.append(cur.getMessage());
      }
      cur = cur.getCause();
    }
    return sb.length() > 0 ? sb.toString() : e.getClass().getSimpleName();
  }

  @FXML private void goToHome()            { switchContent("home.fxml"); }
  @FXML private void goToLogin()           { switchContent("login.fxml"); }
  @FXML private void goToRegister()        { switchContent("register.fxml"); }
  @FXML private void goToSellerDashboard() { switchContent("seller_dashboard.fxml"); }
  @FXML private void goToAdminDashboard()  { switchContent("admin_dashboard.fxml"); }
  @FXML private void handleGoToDeposit()   { switchContent("bidder_deposit.fxml"); }

  @FXML
  private void handleLogout() {
    Request request = new Request(ActionType.LOGOUT, null);
    SocketClient.getInstance().sendRequest(request);
  }

  @FXML
  private void handleNavHome() {
    setActiveMenu(homeNavButton);
    goToHome();
  }

  @FXML
  private void handleNavAuctionRoom() {
    setActiveMenu(auctionRoomButton);
    // Nếu chưa ở trang chủ → về home trước
    if (!(activeContentController instanceof HomeController)) {
      switchContent("home.fxml");
      // Đợi HomeController load xong rồi mới gọi filter + scroll
      Platform.runLater(() -> {
        if (activeContentController instanceof HomeController homeCtrl) {
          homeCtrl.scrollToAndFilterActiveAuctions();
        }
      });
    } else {
      // Đã ở trang chủ → gọi trực tiếp filter + scroll
      HomeController homeCtrl = (HomeController) activeContentController;
      homeCtrl.scrollToAndFilterActiveAuctions();
    }
  }

  @FXML
  private void handleNavAuctionHistory() {
    setActiveMenu(auctionHistoryButton);
    // future route: create auction history screen and navigate here
  }

  private void setActiveMenu(Button activeButton) {
    homeNavButton.getStyleClass().removeAll("nav-link-active");
    auctionRoomButton.getStyleClass().removeAll("nav-link-active");
    auctionHistoryButton.getStyleClass().removeAll("nav-link-active");

    if (!homeNavButton.getStyleClass().contains("nav-link")) {
      homeNavButton.getStyleClass().add("nav-link");
    }
    if (!auctionRoomButton.getStyleClass().contains("nav-link")) {
      auctionRoomButton.getStyleClass().add("nav-link");
    }
    if (!auctionHistoryButton.getStyleClass().contains("nav-link")) {
      auctionHistoryButton.getStyleClass().add("nav-link");
    }

    if (!activeButton.getStyleClass().contains("nav-link-active")) {
      activeButton.getStyleClass().add("nav-link-active");
    }
  }

  private void setupClock() {
    updateDateTime();
    clockTimeline = new Timeline(
      new KeyFrame(Duration.seconds(0), event -> updateDateTime()),
      new KeyFrame(Duration.seconds(1))
    );
    clockTimeline.setCycleCount(Timeline.INDEFINITE);
    clockTimeline.play();
  }

  private void updateDateTime() {
    LocalDateTime now = LocalDateTime.now();
    timeLabel.setText(now.format(TIME_FORMAT));
    dateLabel.setText(now.format(DATE_FORMAT));
  }

  @FXML
  private void handleUpgradeSeller() {
    switchContent("upgrade_seller.fxml");
  }

  @FXML
  private void toggleNotificationMenu() {
    if (notificationMenu.getItems().isEmpty()) {
      MenuItem emptyItem = new MenuItem("Không có thông báo mới");
      notificationMenu.getItems().add(emptyItem);
    }
    if (!notificationMenu.isShowing()) {
      notificationMenu.show(notificationBellButton, javafx.geometry.Side.BOTTOM, 0, 0);
      unreadCount = 0;
      notificationBadge.setVisible(false);
      notificationBadge.setText("0");
    } else {
      notificationMenu.hide();
    }
  }

  /** Format số tiền VND dạng "50,000,000 VND" */
  private String formatVND(double amount) {
    return String.format("💰 %,.0f VND", amount);
  }
}
