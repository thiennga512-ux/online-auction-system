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
import java.util.Arrays;
import java.util.List;
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

  /** Danh sách tất cả các nút điều hướng trên Top Navbar, dùng để quản lý active state */
  private List<Button> navButtons;

  @FXML
  public void initialize() {
    ClientMain.setMainController(this);
    initNavbarButtons();
    updateNavbarHighlight(homeNavButton);

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

          // --- Nút nâng cấp Seller (chỉ Bidder) ---
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

          // --- Role-based visibility: BIDDER / SELLER / ADMIN ---
          if (isBidder) {
            // BIDDER: Hiển thị "Mở Kênh Người Bán", ẩn "Kênh Người Bán", ẩn "Quản trị"
            upgradeSellerButton.setVisible(true);
            upgradeSellerButton.setManaged(true);
            sellerDashboardButton.setVisible(false);
            sellerDashboardButton.setManaged(false);
            adminDashboardButton.setVisible(false);
            adminDashboardButton.setManaged(false);
          } else if (isSeller) {
            // SELLER: Ẩn "Mở Kênh Người Bán", hiển thị "Kênh Người Bán", ẩn "Quản trị"
            upgradeSellerButton.setVisible(false);
            upgradeSellerButton.setManaged(false);
            sellerDashboardButton.setVisible(true);
            sellerDashboardButton.setManaged(true);
            adminDashboardButton.setVisible(false);
            adminDashboardButton.setManaged(false);
          } else if (isAdmin) {
            // ADMIN: Ẩn cả 2 nút kênh người bán, chỉ hiển thị "Quản trị"
            upgradeSellerButton.setVisible(false);
            upgradeSellerButton.setManaged(false);
            sellerDashboardButton.setVisible(false);
            sellerDashboardButton.setManaged(false);
            adminDashboardButton.setVisible(true);
            adminDashboardButton.setManaged(true);
          }
        }
      });
    });

    // ===== QUAN TRỌNG: Khởi động đồng hồ thời gian thực =====
    // setupClock() tạo Timeline INDEFINITE chạy độc lập trên FX Application Thread.
    // Việc load/nạp view con vào contentArea (switchContent) KHÔNG làm ảnh hưởng
    // đến Timeline này vì nó không phụ thuộc vào nội dung contentArea.
    setupClock();

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

  @FXML public void goToHome()            {
    updateNavbarHighlight(homeNavButton);
    switchContent("home.fxml");
  }
  @FXML private void goToLogin()           { switchContent("login.fxml"); }
  @FXML private void goToRegister()        { switchContent("register.fxml"); }
  @FXML private void goToSellerDashboard() {
    updateNavbarHighlight(sellerDashboardButton);
    switchContent("seller_dashboard.fxml");
  }
  @FXML private void goToAdminDashboard()  {
    updateNavbarHighlight(adminDashboardButton);
    switchContent("admin_dashboard.fxml");
  }
  @FXML private void handleGoToDeposit()   {
    updateNavbarHighlight(depositButton);
    switchContent("bidder_deposit.fxml");
  }

  @FXML
  private void handleLogout() {
    Request request = new Request(ActionType.LOGOUT, null);
    SocketClient.getInstance().sendRequest(request);
  }

  @FXML
  private void handleNavHome() {
    goToHome();
  }

  @FXML
  private void handleNavAuctionRoom() {
    updateNavbarHighlight(auctionRoomButton);
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
  private void handleNavAuctionResults() {
    updateNavbarHighlight(auctionHistoryButton);
    switchContent("auction_results.fxml");
  }

  /**
   * Khởi tạo danh sách tất cả các nút điều hướng trên Top Navbar.
   * Gọi một lần duy nhất trong initialize().
   */
  private void initNavbarButtons() {
    navButtons = Arrays.asList(
        homeNavButton,
        auctionRoomButton,
        auctionHistoryButton,
        sellerDashboardButton,
        adminDashboardButton,
        depositButton,
        upgradeSellerButton
    );
    // Bảo đảm tất cả các nút đều có styleClass "nav-button" để CSS áp dụng
    for (Button btn : navButtons) {
      if (!btn.getStyleClass().contains("nav-button")) {
        btn.getStyleClass().add("nav-button");
      }
    }
  }

  /**
   * Helper Method — Cập nhật trạng thái active (nền xanh) cho thanh điều hướng.
   * Duyệt qua tất cả các navButtons, xóa class 'nav-button-active' khỏi tất cả,
   * sau đó chỉ thêm vào đúng nút được truyền vào.
   */
  private void updateNavbarHighlight(Button activeButton) {
    for (Button btn : navButtons) {
      btn.getStyleClass().removeAll("nav-button-active");
      // Bảo đảm vẫn giữ lại class nav-link để style nền tảng
      if (!btn.getStyleClass().contains("nav-link")) {
        btn.getStyleClass().add("nav-link");
      }
    }
    if (activeButton != null && !activeButton.getStyleClass().contains("nav-button-active")) {
      activeButton.getStyleClass().add("nav-button-active");
      // Xóa nav-link-active cũ nếu có để tránh xung đột CSS
      activeButton.getStyleClass().removeAll("nav-link-active");
    }
  }

  /**
   * ===== ĐỒNG HỒ THỜI GIAN THỰC TRÊN NAVBAR =====
   * 
   * Tạo Timeline INDEFINITE cập nhật timeLabel và dateLabel mỗi giây.
   * 
   * Quan trọng: Timeline này chạy độc lập trên FX Application Thread,
   * KHÔNG phụ thuộc vào contentArea hay bất kỳ view con nào.
   * Việc gọi switchContent() để load view con vào contentArea
   * KHÔNG làm ảnh hưởng đến vòng lặp đồng hồ này.
   * 
   * Timeline được khởi động một lần duy nhất trong initialize()
   * và chạy mãi mãi (setCycleCount(INDEFINITE)).
   */
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
    updateNavbarHighlight(upgradeSellerButton);
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