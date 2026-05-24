package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.common.dto.Dto;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.common.network.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import javafx.animation.PauseTransition;
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

  @FXML
  public void initialize() {
    setupTableColumns();
    styleTable();
    SocketClient.getInstance().addListener(responseListener);
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
    // Chú ý: KHÔNG tạo Label graphic cho header để tránh lặp tiêu đề.
    // Tiêu đề chỉ được lấy từ text attribute trong FXML.
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
    // Remove default alternating row colors - we handle via CSS
    resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    resultsTable.setPlaceholder(new Label("📭 Chưa có phiên đấu giá nào kết thúc."));
  }

  /**
   * Center-align a column's header and data cells.
   * KHÔNG tạo Label graphic cho header — chỉ dùng text từ FXML, tránh lặp tiêu đề.
   */
  private void centerColumn(TableColumn<AuctionResult, String> column) {
    // Center via CSS cho header
    column.setStyle("-fx-alignment: CENTER;");

    // Center data cells
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
  private void loadAuctionResults() {
    showLoading(true);
    hideError();
    
    // Request auction results from server
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_AUCTION_RESULTS, null));
  }

  private void handleResponse(Response response) {
    if (response.getActionType() == ActionType.GET_AUCTION_RESULTS) {
      Platform.runLater(() -> {
        showLoading(false);
        restoreRefreshButton();
        
        if (!response.isSuccess()) {
          showError("Không thể tải kết quả đấu giá: " + response.getMessage());
          loadMockData();
          return;
        }

        List<Dto.AuctionResultDto> results = response.getDataAsList(Dto.AuctionResultDto.class);
        if (results == null || results.isEmpty()) {
          loadMockData();
          return;
        }

        populateTable(results);
      });
    }
  }

  /**
   * Populate table from server data.
   * Lấy TẤT CẢ các phiên đã kết thúc: SUCCESS, FAILED, CANCELED, REJECTED.
   */
  private void populateTable(List<Dto.AuctionResultDto> results) {
    auctionResults.clear();
    
    int totalSessions = results.size();
    int successCount = 0;

    for (Dto.AuctionResultDto dto : results) {
      // Parse endTime
      String displayEndTime;
      try {
        LocalDateTime ldt = LocalDateTime.parse(dto.endTime());
        displayEndTime = ldt.format(DISPLAY_FORMATTER);
      } catch (Exception e) {
        displayEndTime = dto.endTime(); // fallback to raw string
      }

      // Xác định trạng thái text dựa trên status từ server
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
        case "CANCELED" -> {
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

    // Update dashboard badges
    updateDashboard(totalSessions, successCount);
  }

  /**
   * Load mock data for demonstration when server is unavailable.
   * Sử dụng đúng trạng thái: "Thành Công", "Thất Bại", "Bị Hủy"
   */
  private void loadMockData() {
    auctionResults.clear();
    
    auctionResults.addAll(
      new AuctionResult("15/05/2026 14:30:00", "Đồng hồ Rolex Submariner 2025",           "Thành Công", ""),
      new AuctionResult("14/05/2026 09:15:00", "iPhone 16 Pro Max 1TB",                    "Thất Bại",   "Không có người đặt giá"),
      new AuctionResult("12/05/2026 20:00:00", "Tranh sơn dầu 'Hoàng hôn Đà Lạt'",         "Thành Công", ""),
      new AuctionResult("10/05/2026 10:30:00", "Xe máy Honda SH 2025",                     "Thất Bại",   "Giá sàn chưa được đáp ứng"),
      new AuctionResult("08/05/2026 16:45:00", "Bộ sưu tập tiền cổ quý hiếm",              "Thành Công", ""),
      new AuctionResult("05/05/2026 11:00:00", "Laptop MSI Gaming GT77",                   "Bị Hủy",     "Phiên bị hủy bởi Admin"),
      new AuctionResult("01/05/2026 08:30:00", "Bức tượng Phật ngọc bích",                  "Thành Công", ""),
      new AuctionResult("28/04/2026 15:00:00", "Máy ảnh Sony A7R V + Lens 24-70",          "Thất Bại",   "Không có người đặt giá"),
      new AuctionResult("25/04/2026 19:30:00", "Đàn guitar acoustic Gibson 1960",           "Thành Công", ""),
      new AuctionResult("20/04/2026 10:00:00", "Đồng hồ Casio G-Shock MRG-B2000B",         "Bị Hủy",     "Phiên bị hủy bởi Admin")
    );

    // Update dashboard with mock data: 10 phiên, 4 thành công
    updateDashboard(auctionResults.size(), 4);
  }

  /**
   * Update the dashboard badges with statistics
   */
  private void updateDashboard(int totalSessions, int successCount) {
    totalSessionsValue.setText(String.valueOf(totalSessions));
    
    int successRate = totalSessions > 0 ? (successCount * 100 / totalSessions) : 0;
    successRateValue.setText(successRate + "%");
    
    // Color-code the success rate
    if (successRate >= 70) {
      successRateValue.setStyle("-fx-text-fill: #10b981; -fx-font-size: 28px; -fx-font-weight: bold;");
    } else if (successRate >= 40) {
      successRateValue.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 28px; -fx-font-weight: bold;");
    } else {
      successRateValue.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 28px; -fx-font-weight: bold;");
    }
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

  @Override
  public void onBeforeHide() {
    SocketClient.getInstance().removeListener(responseListener);
  }

  @FXML
  private void handleRefresh() {
    if (refreshButton != null) {
      refreshButton.setText("⏳ Đang tải...");
      refreshButton.setDisable(true);
    }
    loadAuctionResults();
  }
}