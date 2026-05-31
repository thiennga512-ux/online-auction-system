package com.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.network.Request;
import com.auction.network.Response;
import com.network.SocketClient;
import com.util.SessionManager;

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
 * - Tab 1: Quản lý phiên đấu giá (Duyệt / Từ chối / Huỷ)
 * - Tab 2: Quản lý user (Xem, Khoá/Mở khoá)
 * - Tab 3: Duyệt yêu cầu nạp tiền từ Bidder
 * ============================================================
 */
public class AdminDashboardController implements LifeCycleAwareController {

  // --- Tab 1: Phiên đấu giá ---
  @FXML
  private Label adminNameLabel;
  @FXML
  private Label totalActiveAuctionsLabel;
  @FXML
  private Label lastUpdatedLabel;
  @FXML
  private ListView<String> activeAuctionsListView;
  @FXML
  private ListView<String> pendingAuctionsListView;
  @FXML
  private TextField rejectReasonField;
  @FXML
  private Label adminActionMessageLabel;

  // --- Tab 2: Quản lý User ---
  @FXML
  private ListView<String> usersListView;
  @FXML
  private Label userActionMessageLabel;
  @FXML
  private Label pendingCountLabel; // KPI card: số phiên PENDING
  @FXML
  private TextField depositAmountField; // Ô nhập số tiền nạp cho Bidder

  // --- Tab 3: Duyệt nạp tiền (cũ) — giữ lại để không lỗi khi render cũ ---
  @FXML
  private ListView<String> pendingDepositsListView;
  @FXML
  private TextField depositRejectReasonField;
  @FXML
  private Label depositActionMessageLabel;
  @FXML
  private Label pendingDepositCountLabel;

  private final Consumer<Response> listener = this::handleResponse;

  // Maps: display string → ID
  private final Map<String, String> pendingDisplayToSessionId = new HashMap<>();
  private final Map<String, String> activeDisplayToSessionId = new HashMap<>();
  private final Map<String, String> userDisplayToId = new HashMap<>();
  private final Map<String, String> depositDisplayToRequestId = new HashMap<>(); // Tab 3

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
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_PENDING_DEPOSITS, null));
  }

  private void handleResponse(Response response) {
    switch (response.getActionType()) {
      case GET_ACTIVE_AUCTIONS -> renderActiveAuctions(response);
      case GET_PENDING_AUCTIONS -> renderPendingAuctions(response);
      case APPROVE_AUCTION, REJECT_AUCTION, CANCEL_AUCTION -> handleModerationResponse(response);
      case GET_ALL_USERS -> renderUsers(response);
      case SET_USER_ACTIVE -> handleUserActiveResponse(response);
      case GET_PENDING_DEPOSITS -> renderPendingDeposits(response);
      case APPROVE_DEPOSIT -> handleDepositActionResponse(response, true);
      case REJECT_DEPOSIT -> handleDepositActionResponse(response, false);
      case ADMIN_DEPOSIT -> handleAdminDepositResponse(response);
      case DEPOSIT_REQUEST_BROADCAST -> Platform
          .runLater(() -> SocketClient.getInstance().sendRequest(new Request(ActionType.GET_PENDING_DEPOSITS, null)));
      case NEW_AUCTION_BROADCAST, AUCTION_STARTED_BROADCAST, AUCTION_ENDED_BROADCAST ->
        Platform.runLater(this::refreshData);
      default -> {
        /* ignore */ }
    }
  }

  // -------------------------------------------------------
  // TAB 1: PHIÊN ĐẤU GIÁ
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
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.APPROVE_AUCTION,
        new Dto.ApproveAuctionRequest(sessionId)));
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
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.REJECT_AUCTION,
        new Dto.RejectAuctionRequest(sessionId, reason)));
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
    if (sessionId == null)
      return;

    // Hỏi lý do huỷ
    javafx.scene.control.TextInputDialog reasonDialog = new javafx.scene.control.TextInputDialog();
    reasonDialog.setTitle("Lý do huỷ phiên");
    reasonDialog.setHeaderText("Cảnh báo: Huỷ phiên đấu giá đang chạy!");
    reasonDialog.setContentText("Vui lòng nhập lý do huỷ:");
    java.util.Optional<String> result = reasonDialog.showAndWait();
    if (result.isEmpty() || result.get().isBlank()) {
      adminActionMessageLabel.setStyle("-fx-text-fill: #ef4444;");
      adminActionMessageLabel.setText("❌ Cần nhập lý do để huỷ phiên đấu giá.");
      return;
    }
    String reason = result.get().trim();

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận huỷ phiên");
    confirm.setHeaderText("Bạn có chắc chắn muốn huỷ phiên này?");
    confirm.setContentText(selected + "\nLý do: " + reason);

    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
      SocketClient.getInstance().sendRequest(new Request(
          ActionType.CANCEL_AUCTION,
          new Dto.CancelAuctionRequest(sessionId, reason)));
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

      // Cập nhật KPI badge số phiên pending (mới)
      if (pendingCountLabel != null) {
        pendingCountLabel.setText(String.valueOf(sessions.size()));
      }

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
  // TAB 2: QUẢN LÝ USER
  // -------------------------------------------------------

  private void renderUsers(Response response) {
    Platform.runLater(() -> {
      userDisplayToId.clear();
      if (usersListView != null)
        usersListView.getItems().clear();
      if (!response.isSuccess()) {
        if (usersListView != null)
          usersListView.getItems().add("Không tải được danh sách user.");
        return;
      }
      List<Dto.UserSummaryResponse> users = response.getDataAsList(Dto.UserSummaryResponse.class);
      if (usersListView == null)
        return;
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
            u.active() ? "| ✅ ACTIVE" : "| 🔒 LOCKED");
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
    if (userId == null)
      return;

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận khoá tài khoản");
    confirm.setContentText("Khoá tài khoản: " + selected + " ?");
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.SET_USER_ACTIVE, new Dto.SetUserActiveRequest(userId, false)));
  }

  @FXML
  private void handleUnlockUser() {
    String selected = usersListView == null ? null : usersListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      setUserActionMessage("Vui lòng chọn user.", false);
      return;
    }
    String userId = userDisplayToId.get(selected);
    if (userId == null)
      return;

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.SET_USER_ACTIVE, new Dto.SetUserActiveRequest(userId, true)));
  }

  private void handleUserActiveResponse(Response response) {
    Platform.runLater(() -> {
      if (response.isSuccess())
        setUserActionMessage("✅ " + response.getMessage(), true);
      else
        setUserActionMessage("❌ " + response.getMessage(), false);
      handleRefreshUsers();
    });
  }

  private void setUserActionMessage(String msg, boolean success) {
    if (userActionMessageLabel != null) {
      userActionMessageLabel.setStyle(success ? "-fx-text-fill: #10b981;" : "-fx-text-fill: #ef4444;");
      userActionMessageLabel.setText(msg);
    }
  }

  /** Admin nạp tiền trực tiếp cho Bidder đang chọn */
  @FXML
  private void handleDepositForUser() {
    String selected = usersListView == null ? null : usersListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      setUserActionMessage("Vui lòng chọn user trong danh sách.", false);
      return;
    }
    String userId = userDisplayToId.get(selected);
    if (userId == null)
      return;

    if (depositAmountField == null || depositAmountField.getText().isBlank()) {
      setUserActionMessage("Vui lòng nhập số tiền cần nạp.", false);
      return;
    }
    double amount;
    try {
      amount = Double.parseDouble(depositAmountField.getText().trim().replace(",", ""));
      if (amount <= 0) {
        setUserActionMessage("Số tiền phải lớn hơn 0.", false);
        return;
      }
    } catch (NumberFormatException e) {
      setUserActionMessage("Số tiền không hợp lệ.", false);
      return;
    }

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận nạp tiền");
    confirm.setContentText(String.format("Nạp %,.0f VND cho:\n%s", amount, selected));
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.ADMIN_DEPOSIT, new Dto.DepositRequest(userId, amount)));
  }

  private void handleAdminDepositResponse(Response response) {
    Platform.runLater(() -> {
      if (response.isSuccess()) {
        setUserActionMessage("✅ " + response.getMessage(), true);
        if (depositAmountField != null)
          depositAmountField.clear();
        handleRefreshUsers();
      } else {
        setUserActionMessage("❌ " + response.getMessage(), false);
      }
    });
  }

  // -------------------------------------------------------
  // TAB 3: DUYỆT YÊU CẦU NẠP TIỀN
  // -------------------------------------------------------

  @FXML
  private void handleRefreshPendingDeposits() {
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_PENDING_DEPOSITS, null));
  }

  private void renderPendingDeposits(Response response) {
    Platform.runLater(() -> {
      depositDisplayToRequestId.clear();
      if (pendingDepositsListView != null)
        pendingDepositsListView.getItems().clear();
      if (!response.isSuccess()) {
        if (pendingDepositsListView != null)
          pendingDepositsListView.getItems().add("Không tải được danh sách yêu cầu.");
        return;
      }
      List<Dto.PendingDepositDto> deposits = response.getDataAsList(Dto.PendingDepositDto.class);

      // Cập nhật badge số lượng
      if (pendingDepositCountLabel != null) {
        pendingDepositCountLabel.setText(deposits.isEmpty()
            ? "Không có yêu cầu nào"
            : deposits.size() + " yêu cầu đang chờ");
        pendingDepositCountLabel.setStyle(deposits.isEmpty()
            ? "-fx-font-size: 13px; -fx-text-fill: #6b7280; -fx-background-color: rgba(107,114,128,0.15); -fx-padding: 5 12; -fx-background-radius: 20;"
            : "-fx-font-size: 13px; -fx-text-fill: #fbbf24; -fx-background-color: rgba(251,191,36,0.15); -fx-padding: 5 12; -fx-background-radius: 20;");
      }

      if (pendingDepositsListView == null)
        return;
      if (deposits.isEmpty()) {
        pendingDepositsListView.getItems().add("✅ Không có yêu cầu nạp tiền nào đang chờ duyệt.");
        return;
      }
      for (Dto.PendingDepositDto d : deposits) {
        String row = String.format("[%s...]  %-20s  %-30s  %,.0f VND  |  Gửi lúc: %s",
            d.requestId().substring(0, 8),
            d.bidderName(),
            d.bidderEmail(),
            d.amount(),
            d.requestedAt());
        depositDisplayToRequestId.put(row, d.requestId());
        pendingDepositsListView.getItems().add(row);
      }
    });
  }

  @FXML
  private void handleApproveDeposit() {
    String selected = pendingDepositsListView == null ? null
        : pendingDepositsListView.getSelectionModel().getSelectedItem();
    if (selected == null || !depositDisplayToRequestId.containsKey(selected)) {
      setDepositActionMessage("❌ Vui lòng chọn một yêu cầu trong danh sách.", false);
      return;
    }
    String requestId = depositDisplayToRequestId.get(selected);

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận duyệt nạp tiền");
    confirm.setHeaderText("Bạn có chắc muốn duyệt yêu cầu này?");
    confirm.setContentText("Tiền sẽ được ghi có ngay vào tài khoản người dùng:\n" + selected.trim());
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.APPROVE_DEPOSIT,
        new Dto.ApproveDepositRequest(requestId)));
  }

  @FXML
  private void handleRejectDeposit() {
    String selected = pendingDepositsListView == null ? null
        : pendingDepositsListView.getSelectionModel().getSelectedItem();
    if (selected == null || !depositDisplayToRequestId.containsKey(selected)) {
      setDepositActionMessage("❌ Vui lòng chọn một yêu cầu trong danh sách.", false);
      return;
    }
    String requestId = depositDisplayToRequestId.get(selected);
    String reason = depositRejectReasonField == null ? "" : depositRejectReasonField.getText().trim();
    if (reason.isBlank()) {
      setDepositActionMessage("❌ Vui lòng nhập lý do từ chối.", false);
      return;
    }

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận từ chối");
    confirm.setHeaderText("Từ chối yêu cầu nạp tiền");
    confirm.setContentText("Lý do: " + reason + "\n" + selected.trim());
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;

    SocketClient.getInstance().sendRequest(new Request(
        ActionType.REJECT_DEPOSIT,
        new Dto.RejectDepositRequest(requestId, reason)));
  }

  private void handleDepositActionResponse(Response response, boolean isApprove) {
    Platform.runLater(() -> {
      if (response.isSuccess()) {
        setDepositActionMessage("✅ " + response.getMessage(), true);
        if (depositRejectReasonField != null)
          depositRejectReasonField.clear();
        // Tải lại danh sách sau khi xử lý
        handleRefreshPendingDeposits();
      } else {
        setDepositActionMessage("❌ " + response.getMessage(), false);
      }
    });
  }

  private void setDepositActionMessage(String msg, boolean success) {
    if (depositActionMessageLabel != null) {
      depositActionMessageLabel.setStyle(success ? "-fx-text-fill: #10b981;" : "-fx-text-fill: #ef4444;");
      depositActionMessageLabel.setText(msg);
    }
  }

  // -------------------------------------------------------
  // LIFECYCLE
  // -------------------------------------------------------

  @Override
  public void onBeforeHide() {
    SocketClient.getInstance().removeListener(listener);
  }
}
