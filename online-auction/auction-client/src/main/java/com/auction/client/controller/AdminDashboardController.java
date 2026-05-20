package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.util.SessionManager;
import com.auction.common.dto.Dto;

import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * ============================================================
 * AdminDashboardController — Bảng điều khiển Quản trị
 * ============================================================
 *
 * Chức năng:
 *   - Xem phiên đấu giá đang hoạt động
 *   - Duyệt / Từ chối phiên đấu giá PENDING
 *   - Quản lý user: xem danh sách, khoá / mở khoá, nạp tiền
 * ============================================================
 */
public class AdminDashboardController implements LifecycleAwareController {

  // --- Phần Overview ---
  @FXML private Label adminNameLabel;
  @FXML private Label totalActiveAuctionsLabel;
  @FXML private Label lastUpdatedLabel;

  // --- Phần Phiên đấu giá ---
  @FXML private ListView<String> activeAuctionsListView;
  @FXML private ListView<String> pendingAuctionsListView;
  @FXML private TextField rejectReasonField;
  @FXML private Label adminActionMessageLabel;

  // --- Phần Quản lý User ---
  @FXML private ListView<String> usersListView;
  @FXML private TextField depositAmountField;
  @FXML private Label userActionMessageLabel;

  private final Consumer<Response> listener = this::handleResponse;
  private final Map<String, String> pendingDisplayToSessionId = new HashMap<>();
  private final Map<String, String> activeDisplayToSessionId = new HashMap<>();
  // Map: display string → userId (cho quản lý user)
  private final Map<String, String> userDisplayToId = new HashMap<>();

  @FXML
  public void initialize() {
    var user = SessionManager.getInstance().getCurrentUser();
    if (user != null) {
      adminNameLabel.setText("Quản trị viên: " + user.fullName());
    }

    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);
    refreshData();
  }

  @FXML
  private void handleRefresh() {
    refreshData();
  }

  private void refreshData() {
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ACTIVE_AUCTIONS, null));
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_PENDING_AUCTIONS, null));
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ALL_USERS, null));
  }

  private void handleResponse(Response response) {
    switch (response.getActionType()) {
      case GET_ACTIVE_AUCTIONS -> renderActiveAuctions(response);
      case GET_PENDING_AUCTIONS -> renderPendingAuctions(response);
      case APPROVE_AUCTION, REJECT_AUCTION, CANCEL_AUCTION -> handleModerationResponse(response);
      case GET_ALL_USERS -> renderUsers(response);
      case SET_USER_ACTIVE -> handleUserActiveResponse(response);
      case DEPOSIT_BALANCE -> handleDepositResponse(response);
      case NEW_AUCTION_BROADCAST, AUCTION_STARTED_BROADCAST, AUCTION_ENDED_BROADCAST -> Platform.runLater(this::refreshData);
      default -> { /* ignore */ }
    }
  }

  // -------------------------------------------------------
  // PHIÊN ĐẤU GIÁ
  // -------------------------------------------------------

  @FXML
  private void handleApproveSelected() {
    String selected = pendingAuctionsListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      adminActionMessageLabel.setText("Vui lòng chọn một phiên chờ duyệt.");
      return;
    }
    String sessionId = pendingDisplayToSessionId.get(selected);
    if (sessionId == null) {
      adminActionMessageLabel.setText("Không lấy được sessionId của phiên đã chọn.");
      return;
    }

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận duyệt phiên");
    confirm.setHeaderText("Bạn có chắc muốn duyệt phiên này?");
    confirm.setContentText(selected);
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
      return;
    }

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.APPROVE_AUCTION,
        new Dto.ApproveAuctionRequest(sessionId)
    ));
  }

  @FXML
  private void handleRejectSelected() {
    String selected = pendingAuctionsListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      adminActionMessageLabel.setText("Vui lòng chọn một phiên chờ duyệt.");
      return;
    }
    String sessionId = pendingDisplayToSessionId.get(selected);
    if (sessionId == null) {
      adminActionMessageLabel.setText("Không lấy được sessionId của phiên đã chọn.");
      return;
    }
    String reason = rejectReasonField.getText() == null ? "" : rejectReasonField.getText().trim();
    if (reason.isBlank()) {
      adminActionMessageLabel.setText("Vui lòng nhập lý do từ chối.");
      return;
    }

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận từ chối phiên");
    confirm.setHeaderText("Bạn có chắc muốn từ chối phiên này?");
    confirm.setContentText(selected + "\nLý do: " + reason);
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
      return;
    }

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.REJECT_AUCTION,
        new Dto.RejectAuctionRequest(sessionId, reason)
    ));
  }

  @FXML
  private void handleCancelActiveAuction() {
    String selected = activeAuctionsListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      adminActionMessageLabel.setStyle("-fx-text-fill: #ef4444;");
      adminActionMessageLabel.setText("❌ Vui lòng chọn một phiên đang hoạt động để huỷ.");
      return;
    }

    String sessionId = activeDisplayToSessionId.get(selected);
    if (sessionId == null) return;

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận huỷ phiên");
    confirm.setHeaderText("Cảnh báo: Huỷ phiên đấu giá đang chạy!");
    confirm.setContentText("Bạn có chắc chắn muốn huỷ phiên này không? Mọi lịch sử đặt giá sẽ bị vô hiệu hoá.");
    
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
      SocketClient.getInstance().sendRequest(new Request(
          ActionType.CANCEL_AUCTION,
          new Dto.CancelAuctionRequest(sessionId)
      ));
    }
  }

  private void renderActiveAuctions(Response response) {
    Platform.runLater(() -> {
      if (!response.isSuccess()) {
        totalActiveAuctionsLabel.setText("Phiên đang hoạt động: lỗi tải dữ liệu");
        return;
      }

      List<Dto.AuctionCardDto> sessions = response.getDataAsList(Dto.AuctionCardDto.class);
      totalActiveAuctionsLabel.setText("Phiên đang hoạt động: " + sessions.size());
      lastUpdatedLabel.setText("Cập nhật lúc: " + LocalDateTime.now().withNano(0));

      activeAuctionsListView.getItems().clear();
      activeDisplayToSessionId.clear();
      if (sessions.isEmpty()) {
        activeAuctionsListView.getItems().add("Không có phiên nào đang OPEN/RUNNING.");
        return;
      }

      sessions.forEach(session -> {
        String row = String.format("[%s] %s | Giá: %,.0f VND | Trạng thái: %s",
              session.sessionId().substring(0, 8),
              session.itemName(),
              session.currentPrice(),
              session.status());
        activeDisplayToSessionId.put(row, session.sessionId());
        activeAuctionsListView.getItems().add(row);
      });
    });
  }

  private void renderPendingAuctions(Response response) {
    Platform.runLater(() -> {
      pendingDisplayToSessionId.clear();
      pendingAuctionsListView.getItems().clear();
      if (!response.isSuccess()) {
        pendingAuctionsListView.getItems().add("Không tải được danh sách chờ duyệt.");
        return;
      }

      List<Dto.AuctionCardDto> sessions = response.getDataAsList(Dto.AuctionCardDto.class);
      if (sessions.isEmpty()) {
        pendingAuctionsListView.getItems().add("Không có phiên nào đang chờ duyệt.");
        return;
      }

      for (Dto.AuctionCardDto session : sessions) {
        String row = String.format("[%s] %s | Seller: %s | Bắt đầu: %s",
            session.sessionId().substring(0, 8),
            session.itemName(),
            session.sellerName(),
            session.startTime().substring(0, 16).replace('T', ' '));
        pendingDisplayToSessionId.put(row, session.sessionId());
        pendingAuctionsListView.getItems().add(row);
      }
    });
  }

  private void handleModerationResponse(Response response) {
    Platform.runLater(() -> {
      if (response.isSuccess()) {
        adminActionMessageLabel.setStyle("-fx-text-fill: #10b981;");
        adminActionMessageLabel.setText("✅ " + response.getMessage());
        rejectReasonField.clear();
        refreshData();
      } else {
        adminActionMessageLabel.setStyle("-fx-text-fill: #ef4444;");
        adminActionMessageLabel.setText("❌ " + response.getMessage());
      }
    });
  }

  // -------------------------------------------------------
  // QUẢN LÝ USER
  // -------------------------------------------------------

  private void renderUsers(Response response) {
    Platform.runLater(() -> {
      userDisplayToId.clear();
      if (usersListView != null) usersListView.getItems().clear();
      if (!response.isSuccess()) {
        if (usersListView != null) usersListView.getItems().add("Không tải được danh sách user.");
        return;
      }
      List<Dto.UserSummaryResponse> users = response.getDataAsList(Dto.UserSummaryResponse.class);
      if (usersListView == null) return;
      if (users.isEmpty()) {
        usersListView.getItems().add("Không có user nào.");
        return;
      }
      for (Dto.UserSummaryResponse u : users) {
        String row = String.format("[%s] %s | %s | %s %s",
            u.id().substring(0, 8),
            u.fullName(),
            u.email(),
            u.role(),
            u.active() ? "| ✅ ACTIVE" : "");
        userDisplayToId.put(row, u.id());
        usersListView.getItems().add(row);
      }
    });
  }

  @FXML
  private void handleRefreshUsers() {
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ALL_USERS, null));
  }

  @FXML
  private void handleLockUser() {
    String selected = usersListView == null ? null : usersListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      setUserActionMessage("Vui lòng chọn user.", false);
      return;
    }
    String userId = userDisplayToId.get(selected);
    if (userId == null) return;

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận khoá tài khoản");
    confirm.setContentText("Khoá tài khoản: " + selected + " ?");
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.SET_USER_ACTIVE,
        new Dto.SetUserActiveRequest(userId, false)
    ));
  }

  @FXML
  private void handleUnlockUser() {
    String selected = usersListView == null ? null : usersListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      setUserActionMessage("Vui lòng chọn user.", false);
      return;
    }
    String userId = userDisplayToId.get(selected);
    if (userId == null) return;

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.SET_USER_ACTIVE,
        new Dto.SetUserActiveRequest(userId, true)
    ));
  }

  @FXML
  private void handleDepositForUser() {
    String selected = usersListView == null ? null : usersListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      setUserActionMessage("Vui lòng chọn user (Bidder).", false);
      return;
    }
    String userId = userDisplayToId.get(selected);
    if (userId == null) return;

    String amountStr = depositAmountField == null ? "" : depositAmountField.getText().trim();
    if (amountStr.isEmpty()) {
      setUserActionMessage("Vui lòng nhập số tiền nạp.", false);
      return;
    }
    try {
      double amount = Double.parseDouble(amountStr.replace(",", ""));
      if (amount <= 0) {
        setUserActionMessage("Số tiền phải lớn hơn 0.", false);
        return;
      }
      SocketClient.getInstance().sendRequest(new Request(
          ActionType.DEPOSIT_BALANCE,
          new Dto.DepositRequest(userId, amount)
      ));
    } catch (NumberFormatException e) {
      setUserActionMessage("Số tiền không hợp lệ.", false);
    }
  }

  private void handleUserActiveResponse(Response response) {
    Platform.runLater(() -> {
      if (response.isSuccess()) {
        setUserActionMessage("✅ " + response.getMessage(), true);
      } else {
        setUserActionMessage("❌ " + response.getMessage(), false);
      }
      handleRefreshUsers();
    });
  }

  private void handleDepositResponse(Response response) {
    Platform.runLater(() -> {
      if (response.isSuccess()) {
        setUserActionMessage("✅ " + response.getMessage(), true);
        if (depositAmountField != null) depositAmountField.clear();
      } else {
        setUserActionMessage("❌ " + response.getMessage(), false);
      }
    });
  }

  private void setUserActionMessage(String msg, boolean success) {
    if (userActionMessageLabel != null) {
      userActionMessageLabel.setStyle(success ? "-fx-text-fill: #10b981;" : "-fx-text-fill: #ef4444;");
      userActionMessageLabel.setText(msg);
    }
  }

  @Override
  public void onBeforeHide() {
    SocketClient.getInstance().removeListener(listener);
  }
}
