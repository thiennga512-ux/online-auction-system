package com.Controller;

import com.ClientMain;
import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.network.Request;
import com.network.SocketClient;
import com.util.SessionManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainController {

  @FXML
  private HBox guestBox;
  @FXML
  private HBox userBox;
  @FXML
  private Label greetingLabel;
  @FXML
  private Label balanceLabel;
  @FXML
  private Button upgradeSellerButton;
  @FXML
  private Button depositButton;
  @FXML
  private Button sellerDashboardButton;
  @FXML
  private Button adminDashboardButton;

  @FXML
  private Button homeNavButton;
  @FXML
  private Button auctionRoomButton;
  @FXML
  private Button auctionHistoryButton;
  @FXML
  private Label timeLabel;
  @FXML
  private Label dateLabel;

  @FXML
  private StackPane contentArea;

  @FXML
  private Button notificationBellButton;
  @FXML
  private Label notificationBadge;

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
          boolean isAdmin = "ADMIN".equals(user.role());

          // --- Nút nâng cấp Seller ---
          upgradeSellerButton.setVisible(isBidder);
          upgradeSellerButton.setManaged(isBidder);

          // --- Nút Nạp Tiền (Bidder + Seller) ---
          depositButton.setVisible(isBidder || isSeller);
          depositButton.setManaged(isBidder || isSeller);

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
    // Lắng nghe response từ Server (Logout + Notification + Lock User)
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
      } else if ((response.getActionType() == ActionType.SELF_DEPOSIT
          || response.getActionType() == ActionType.SYNC_BALANCE
          || response.getActionType() == ActionType.DEPOSIT_BALANCE) && response.isSuccess()) {
        // Cập nhật balance label trên header khi có thay đổi số dư
        Platform.runLater(() -> {
          Dto.DepositResultResponse result = response.getDataAs(Dto.DepositResultResponse.class);
          if (result != null) {
            balanceLabel.setText(formatVND(result.newBalance()));
            SessionManager.getInstance().updateBalance(result.newBalance());
          }
        });
      } else if (response.getActionType() == ActionType.SET_USER_ACTIVE && response.isSuccess()) {
        if ("LOCK_USER_NOTIFICATION".equals(response.getMessage())) {
          String lockedUserId = response.getDataAs(String.class);
          var currentUser = SessionManager.getInstance().getCurrentUser();
          if (currentUser != null && currentUser.id().equals(lockedUserId)) {
            Platform.runLater(() -> {
              javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
              alert.setTitle("Tài khoản bị khoá");
              alert.setHeaderText("Cảnh báo bảo mật");
              alert.setContentText("Tài khoản của bạn đã bị khoá bởi Admin! Bạn sẽ bị đăng xuất khỏi hệ thống.");
              alert.showAndWait();

              // Xoá session người dùng và chuyển về trang đăng nhập
              SessionManager.getInstance().setCurrentUser(null);
              goToLogin();
            });
          }
        }
      }
    });  }

  public void switchContent(String fxmlFile) {
    try {
      if (activeContentController instanceof LifeCycleAwareController lifecycleAware) {
        lifecycleAware.onBeforeHide();
      }
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
      Node node = loader.load();
      activeContentController = loader.getController();
      contentArea.getChildren().clear();
      contentArea.getChildren().add(node);
    } catch (Exception e) {
      System.out.println("===== LOAD ERROR =====");
      e.printStackTrace();
    }
  }

  @FXML
  private void goToHome() {
    switchContent("home.fxml");
  }

  @FXML
  private void goToLogin() {
    setActiveMenu(null);
    switchContent("login.fxml");
  }

  @FXML
  private void goToRegister() {
    setActiveMenu(null);
    switchContent("register.fxml");
  }

  @FXML
  private void goToSellerDashboard() {
    setActiveMenu(sellerDashboardButton);
    switchContent("seller_dashboard.fxml");
  }

  @FXML
  private void goToAdminDashboard() {
    setActiveMenu(null);
    switchContent("admin_dashboard.fxml");
  }

  @FXML
  private void handleGoToDeposit() {
    setActiveMenu(null);
    switchContent("bidder_deposit.fxml");
  }

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
    switchContent("auction_results.fxml");
  }

  private void setActiveMenu(Button activeButton) {
    homeNavButton.getStyleClass().removeAll("nav-link-active");
    auctionRoomButton.getStyleClass().removeAll("nav-link-active");
    auctionHistoryButton.getStyleClass().removeAll("nav-link-active");
    if (sellerDashboardButton != null) {
      sellerDashboardButton.getStyleClass().removeAll("nav-link-active");
    }
    if (adminDashboardButton != null) {
      adminDashboardButton.getStyleClass().removeAll("nav-link-active");
    }
    if (upgradeSellerButton != null) {
      upgradeSellerButton.getStyleClass().removeAll("nav-link-active");
    }

    if (!homeNavButton.getStyleClass().contains("nav-link")) {
      homeNavButton.getStyleClass().add("nav-link");
    }
    if (!auctionRoomButton.getStyleClass().contains("nav-link")) {
      auctionRoomButton.getStyleClass().add("nav-link");
    }
    if (!auctionHistoryButton.getStyleClass().contains("nav-link")) {
      auctionHistoryButton.getStyleClass().add("nav-link");
    }

    if (activeButton != null && !activeButton.getStyleClass().contains("nav-link-active")) {
      activeButton.getStyleClass().add("nav-link-active");
    }
  }

  public void setActiveNavAuctionRoom() {
    setActiveMenu(auctionRoomButton);
  }

  private void setupClock() {
    updateDateTime();
    clockTimeline = new Timeline(
        new KeyFrame(Duration.seconds(0), event -> updateDateTime()),
        new KeyFrame(Duration.seconds(1)));
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
    setActiveMenu(upgradeSellerButton);
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
