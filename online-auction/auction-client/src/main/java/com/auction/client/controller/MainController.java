package com.auction.client.controller;

import com.auction.client.ClientMain;
import com.auction.client.network.SocketClient;
import com.auction.client.util.SessionManager;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
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

  @FXML private StackPane contentArea;
  
  @FXML private Button notificationBellButton;
  @FXML private Label notificationBadge;
  
  private ContextMenu notificationMenu = new ContextMenu();
  private int unreadCount = 0;

  private Object activeContentController;

  @FXML
  public void initialize() {
    ClientMain.setMainController(this);

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
      if (activeContentController instanceof LifecycleAwareController lifecycleAware) {
        lifecycleAware.onBeforeHide();
      }
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
      Node node = loader.load();
      activeContentController = loader.getController();
      contentArea.getChildren().clear();
      contentArea.getChildren().add(node);
    } catch (Exception e) {
      System.err.println("Lỗi khi nạp " + fxmlFile);
      e.printStackTrace();
    }
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
