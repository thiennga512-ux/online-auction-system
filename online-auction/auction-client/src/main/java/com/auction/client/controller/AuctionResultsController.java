package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.common.dto.Dto;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.common.network.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class AuctionResultsController implements LifecycleAwareController {

  @FXML private Label totalSessionsValue;
  @FXML private Label successRateValue;
  
  @FXML private TableView<AuctionResult> resultsTable;
  @FXML private TableColumn<AuctionResult, String> colEndTime;
  @FXML private TableColumn<AuctionResult, String> colAuctionName;
  @FXML private TableColumn<AuctionResult, String> colStatus;
  @FXML private TableColumn<AuctionResult, String> colReason;
  @FXML private VBox loadingOverlay;
  @FXML private Label errorLabel;
  @FXML private Button refreshButton;

  private final ObservableList<AuctionResult> auctionResults = FXCollections.observableArrayList();

  private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  private final Consumer<Response> responseListener = this::handleResponse;

  // ===== Periodic polling: tự động refresh mỗi 15 giây khi tab đang active =====
  private Timeline pollingTimeline;
  private static final int POLLING_INTERVAL_SECONDS = 15;

  // ===== Throttle: ngăn gửi quá nhiều request GET_AUCTION_RESULT trong thời gian ngắn =====
  private final AtomicLong lastRefreshRequestTime = new AtomicLong(0);
  private static final long MIN_REFRESH_INTERVAL_MS = 2000; // 2 giây tối thiểu giữa các lần refresh

  @FXML
  public void initialize() {
    setupTableColumns();
    styleTable();
    SocketClient.getInstance().addListener(responseListener);
    startPollingTimer();
    loadAuctionResults();
  }

  // ===== Model =====
  public static class AuctionResult {
    private final String endTime;
    private final String auctionName;
    private final String status; // "Thành Công" | "Thất Bại" | "Bị Hủy"
    private final String reason;

    public AuctionResult(String endTime, String auctionName, String status, String reason) {
      this.endTime = endTime;
      this.auctionName = auctionName;
      this.status = status;
      this.reason = reason;
    }

    public String getEndTime() { return endTime; }
    public String getAuctionName() { return auctionName; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
  }

  // ===== Table Setup =====
  private void setupTableColumns() {
    // Map columns to properties
    colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
    colAuctionName.setCellValueFactory(new PropertyValueFactory<>("auctionName"));
    colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));

    // === CENTER ALIGNMENT FOR ALL COLUMNS (header + data) ===
    centerColumn(colEndTime);
    centerColumn(colAuctionName);
    centerColumn(colStatus);
    centerColumn(colReason);

    // === WRAP TEXT for auction name column ===
    colAuctionName.setCellFactory(column -> new TableCell<AuctionResult, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setText(item);
          setAlignment(Pos.CENTER);
          setWrapText(true);
          setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 13px; -fx-padding: 8 12;");
        }
      }
    });

    // === COLOR-CODED STATUS COLUMN ===
    colStatus.setCellFactory(column -> new TableCell<AuctionResult, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setAlignment(Pos.CENTER);
          Label badge = createStatusBadge(item);
          setGraphic(badge);
          setText(null);
        }
      }
    });

    // === REASON COLUMN with custom styling ===
    colReason.setCellFactory(column -> new TableCell<AuctionResult, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isBlank()) {
          setText(null);
          setGraphic(null);
        } else {
          setText(item);
          setAlignment(Pos.CENTER);
          setWrapText(true);
          setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 13px; -fx-padding: 8 12;");
        }
      }
    });

    // === ROW HEIGHT & CELL PADDING via row factory ===
    resultsTable.setRowFactory(tv -> {
      TableRow<AuctionResult> row = new TableRow<AuctionResult>() {
        @Override
        protected void updateItem(AuctionResult item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null) {
            setStyle("");
          } else {
            setStyle("-fx-cell-size: 48px;");
          }
        }
      };
      // Hover effect - brighten row on mouse over
      row.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
        if (isNowHovered && !row.isEmpty()) {
          row.setStyle("-fx-background-color: rgba(59, 130, 246, 0.10); -fx-cell-size: 48px;");
        } else if (!row.isEmpty()) {
          row.setStyle("-fx-cell-size: 48px;");
        }
      });
      return row;
    });

    resultsTable.setItems(auctionResults);
  }

  private void styleTable() {
    resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    resultsTable.setPlaceholder(new Label("📭 Chưa có phiên đấu giá nào kết thúc."));
  }

  /**
   * Center-align a column's header and data cells.
   */
  private void centerColumn(TableColumn<AuctionResult, String> column) {
    column.setStyle("-fx-alignment: CENTER;");

    column.setCellFactory(new Callback<TableColumn<AuctionResult, String>, TableCell<AuctionResult, String>>() {
      @Override
      public TableCell<AuctionResult, String> call(TableColumn<AuctionResult, String> param) {
        return new TableCell<>() {
          @Override
          protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
              setText(null);
              setGraphic(null);
            } else {
              setText(item);
              setAlignment(Pos.CENTER);
              setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 13px; -fx-padding: 8 12;");
            }
          }
        };
      }
    });
  }

  /**
   * Create a styled badge Label for status column
   * Hỗ trợ: Thành Công (xanh), Thất Bại (đỏ), Bị Hủy (vàng cam)
   */
  private Label createStatusBadge(String status) {
    Label badge = new Label(status);
    badge.setAlignment(Pos.CENTER);
    badge.setMaxWidth(Double.MAX_VALUE);
    badge.setPrefHeight(28);
    
    if ("Thành Công".equals(status)) {
      badge.setStyle(
        "-fx-background-color: rgba(16, 185, 129, 0.15);" +
        "-fx-text-fill: #10b981;" +
        "-fx-font-size: 12px;" +
        "-fx-font-weight: bold;" +
        "-fx-background-radius: 20;" +
        "-fx-border-color: rgba(16, 185, 129, 0.3);" +
        "-fx-border-radius: 20;" +
        "-fx-border-width: 1;" +
        "-fx-padding: 2 14;" +
        "-fx-alignment: CENTER;"
      );
    } else if ("Thất Bại".equals(status) || "Không Thành Công".equals(status)) {
      badge.setStyle(
        "-fx-background-color: rgba(239, 68, 68, 0.15);" +
        "-fx-text-fill: #ef4444;" +
        "-fx-font-size: 12px;" +
        "-fx-font-weight: bold;" +
        "-fx-background-radius: 20;" +
        "-fx-border-color: rgba(239, 68, 68, 0.3);" +
        "-fx-border-radius: 20;" +
        "-fx-border-width: 1;" +
        "-fx-padding: 2 14;" +
        "-fx-alignment: CENTER;"
      );
    } else if ("Bị Hủy".equals(status)) {
      badge.setStyle(
        "-fx-background-color: rgba(245, 158, 11, 0.15);" +
        "-fx-text-fill: #f59e0b;" +
        "-fx-font-size: 12px;" +
        "-fx-font-weight: bold;" +
        "-fx-background-radius: 20;" +
        "-fx-border-color: rgba(245, 158, 11, 0.3);" +
        "-fx-border-radius: 20;" +
        "-fx-border-width: 1;" +
        "-fx-padding: 2 14;" +
        "-fx-alignment: CENTER;"
      );
    } else {
      // Unknown status — gray fallback
      badge.setStyle(
        "-fx-background-color: rgba(107, 114, 128, 0.15);" +
        "-fx-text-fill: #9ca3af;" +
        "-fx-font-size: 12px;" +
        "-fx-font-weight: bold;" +
        "-fx-background-radius: 20;" +
        "-fx-border-color: rgba(107, 114, 128, 0.3);" +
        "-fx-border-radius: 20;" +
        "-fx-border-width: 1;" +
        "-fx-padding: 2 14;" +
        "-fx-alignment: CENTER;"
      );
    }
    return badge;
  }

  // ===== Data Loading =====
  /**
   * Gửi request lên Server để lấy danh sách kết quả đấu giá.
   * Có cơ chế throttle: nếu request trước đó cách đây chưa đầy 2 giây thì bỏ qua.
   */
  private void loadAuctionResults() {
    long now = System.currentTimeMillis();
    long last = lastRefreshRequestTime.get();
    if (now - last < MIN_REFRESH_INTERVAL_MS) {
      // Đã có request gần đây, bỏ qua để tránh spam
      System.out.println("[AuctionResultsController] Throttle: bỏ qua refresh, chưa đủ " + MIN_REFRESH_INTERVAL_MS + "ms từ lần cuối");
      return;
    }
    lastRefreshRequestTime.set(now);

    showLoading(true);
    hideError();
    
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_AUCTION_RESULTS, null));
  }

  private void handleResponse(Response response) {
    ActionType action = response.getActionType();
    
    // ===== Xử lý broadcast: có phiên kết thúc/bị hủy → tự động refresh =====
    if (action == ActionType.AUCTION_RESULTS_UPDATE_BROADCAST) {
      System.out.println("[AuctionResultsController] Nhận AUCTION_RESULTS_UPDATE_BROADCAST: " + response.getMessage());
      Platform.runLater(this::loadAuctionResults);
      return;
    }
    
    // ===== Xử lý broadcast: Server ép buộc refresh toàn bộ danh sách =====
    if (action == ActionType.SERVER_BROADCAST_REFRESH_RESULTS) {
      System.out.println("[AuctionResultsController] Nhận SERVER_BROADCAST_REFRESH_RESULTS: " + response.getMessage());
      Platform.runLater(this::loadAuctionResults);
      return;
    }
    
    // ===== Xử lý response kết quả đấu giá =====
    if (action == ActionType.GET_AUCTION_RESULTS) {
      Platform.runLater(() -> {
        showLoading(false);
        restoreRefreshButton();
        
        if (!response.isSuccess()) {
          showError("Không thể tải kết quả đấu giá: " + response.getMessage());
          return; // Không load mock data — giữ nguyên dữ liệu cũ nếu có
        }

        List<Dto.AuctionResultDto> results = response.getDataAsList(Dto.AuctionResultDto.class);
        if (results == null || results.isEmpty()) {
          // Server trả về danh sách rỗng — xóa bảng và cập nhật dashboard
          auctionResults.clear();
          updateDashboard(0, 0);
          return;
        }

        populateTable(results);
      });
    }
  }

  /**
   * Populate table from server data.
   */
  private void populateTable(List<Dto.AuctionResultDto> results) {
    auctionResults.clear();
    
    int totalSessions = results.size();
    int successCount = 0;

    for (Dto.AuctionResultDto dto : results) {
      String displayEndTime;
      try {
        LocalDateTime ldt = LocalDateTime.parse(dto.endTime());
        displayEndTime = ldt.format(DISPLAY_FORMATTER);
      } catch (Exception e) {
        displayEndTime = dto.endTime();
      }

      String statusText;
      String reasonText;
      String rawStatus = dto.status() != null ? dto.status().toUpperCase() : "";
      
      switch (rawStatus) {
        case "SUCCESS" -> {
          statusText = "Thành Công";
          reasonText = "";
          successCount++;
        }
        case "FAILED" -> {
          statusText = "Thất Bại";
          reasonText = (dto.reason() != null && !dto.reason().isBlank())
              ? dto.reason() : "Không có người đặt giá";
        }
        case "CANCELLED" -> {
          statusText = "Bị Hủy";
          reasonText = (dto.reason() != null && !dto.reason().isBlank())
              ? dto.reason() : "Phiên bị hủy bởi Admin";
        }
        case "REJECTED" -> {
          statusText = "Bị Hủy";
          reasonText = (dto.reason() != null && !dto.reason().isBlank())
              ? dto.reason() : "Phiên bị hủy bởi Admin";
        }
        default -> {
          statusText = "Không Thành Công";
          reasonText = dto.reason() != null ? dto.reason() : "";
        }
      }

      auctionResults.add(new AuctionResult(displayEndTime, dto.auctionName(), statusText, reasonText));
    }

    updateDashboard(totalSessions, successCount);
  }

  /**
   * Update the dashboard badges with statistics
   */
  private void updateDashboard(int totalSessions, int successCount) {
    totalSessionsValue.setText(String.valueOf(totalSessions));
    
    int successRate = totalSessions > 0 ? (successCount * 100 / totalSessions) : 0;
    successRateValue.setText(successRate + "%");
    
    if (successRate >= 70) {
      successRateValue.setStyle("-fx-text-fill: #10b981; -fx-font-size: 28px; -fx-font-weight: bold;");
    } else if (successRate >= 40) {
      successRateValue.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 28px; -fx-font-weight: bold;");
    } else {
      successRateValue.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 28px; -fx-font-weight: bold;");
    }
  }

  // ===== Periodic Polling =====
  /**
   * Khởi động Timeline tự động refresh mỗi POLLING_INTERVAL_SECONDS giây.
   * Đây là cơ chế dự phòng khi broadcast bị miss (mạng chập chờn, race condition, ...).
   * Timeline tự động dừng khi tab bị ẩn (onBeforeHide).
   */
  private void startPollingTimer() {
    if (pollingTimeline != null) {
      pollingTimeline.stop();
    }
    pollingTimeline = new Timeline(
      new KeyFrame(Duration.seconds(POLLING_INTERVAL_SECONDS), event -> {
        // Chỉ refresh nếu bảng đã được load lần đầu (tránh refresh khi chưa có dữ liệu)
        if (!auctionResults.isEmpty() || totalSessionsValue.getText().equals("0")) {
          System.out.println("[AuctionResultsController] Polling: tự động làm mới dữ liệu...");
          loadAuctionResults();
        }
      })
    );
    pollingTimeline.setCycleCount(Timeline.INDEFINITE);
    pollingTimeline.play();
    System.out.println("[AuctionResultsController] Đã khởi động polling timer (" + POLLING_INTERVAL_SECONDS + "s)");
  }

  // ===== UI Helpers =====
  private void showLoading(boolean show) {
    if (loadingOverlay != null) {
      loadingOverlay.setVisible(show);
      loadingOverlay.setManaged(show);
    }
  }

  private void showError(String message) {
    if (errorLabel != null) {
      errorLabel.setText("⚠️ " + message);
      errorLabel.setVisible(true);
      errorLabel.setManaged(true);
    }
  }

  private void hideError() {
    if (errorLabel != null) {
      errorLabel.setVisible(false);
      errorLabel.setManaged(false);
    }
  }

  private void restoreRefreshButton() {
    if (refreshButton != null) {
      refreshButton.setText("🔄 Làm mới");
      refreshButton.setDisable(false);
    }
  }

  // ===== Lifecycle =====
  @Override
  public void onBeforeHide() {
    // Dừng polling timer để tránh gửi request ngầm khi tab không hiển thị
    if (pollingTimeline != null) {
      pollingTimeline.stop();
      pollingTimeline = null;
      System.out.println("[AuctionResultsController] Đã dừng polling timer (tab bị ẩn)");
    }
    // Hủy đăng ký listener để tránh xử lý response khi không còn active
    SocketClient.getInstance().removeListener(responseListener);
  }

  @FXML
  private void handleRefresh() {
    if (refreshButton != null) {
      refreshButton.setText("⏳ Đang tải...");
      refreshButton.setDisable(true);
    }
    // Reset throttle để đảm bảo refresh khi người dùng bấm nút
    lastRefreshRequestTime.set(0);
    loadAuctionResults();
  }
}