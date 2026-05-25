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
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
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
 * AuctionDetailController — Màn hình Phòng Đấu Giá Trực Tiếp (TabPane Version).
 *
 * Gồm 4 Tab ở khu vực dưới:
 *   1. Lịch sử đặt giá (TableView)
 *   2. Thông tin chi tiết (GridPane)
 *   3. Biểu đồ đấu giá (AreaChart) — lazy-load khi chọn tab
 *   4. AutoBid (Form kích hoạt tự động)
 */
public class AuctionDetailController implements LifecycleAwareController {

  private static final String EMPTY_BID_MSG = "Chưa có lượt đặt giá nào.";
  private static final double BID_ROW_HEIGHT = 52;

  // ===== FXML (Kế thừa từ phiên bản cũ) =====
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
  @FXML private ListView<String> bidHistoryList; // Vẫn giữ cho tương thích, dùng bidHistoryTable là chính
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

  // ===== FXML — TabPane & Components Mới =====
  @FXML private TabPane           auctionTabPane;
  @FXML private TableView<BidRow> bidHistoryTable;
  @FXML private TableColumn<BidRow, String> timeColumn;
  @FXML private TableColumn<BidRow, String> bidderColumn;
  @FXML private TableColumn<BidRow, String> amountColumn;
  @FXML private TableColumn<BidRow, String> typeColumn;
  @FXML private Label            bidTableStatusLabel;

  @FXML private GridPane         specsGrid;

  @FXML private AreaChart<String, Number> priceChart;
  @FXML private CategoryAxis     chartXAxis;
  @FXML private NumberAxis       chartYAxis;
  @FXML private Label            chartPlaceholder;

  @FXML private TextField autobidCeilingField;
  @FXML private TextField autobidIncrementField;
  @FXML private Button    activateAutobidBtn;
  @FXML private Label     autobidStatusLabel;

  // ===== State =====
  private String        sessionId;
  private String        currentStatus;
  private LocalDateTime currentStartTime;
  private LocalDateTime currentEndTime;
  private double        currentPrice  = 0;
  private double        minIncrement  = 100_000;
  private boolean       isNotificationRegistered = false;
  private int           watcherEstimate = 24;

  // Dữ liệu phiên đầy đủ (cached từ response)
  private Dto.AuctionCardDto currentSessionData;

  // Danh sách bid (cached để vẽ biểu đồ)
  private final ObservableList<Bid> allBids = FXCollections.observableArrayList();
  private final ObservableList<BidRow> bidTableData = FXCollections.observableArrayList();

  // AutoBid state
  private boolean autobidActive = false;

  private Timeline countdownTimeline;
  private Timeline pulseTimeline;
  private static final DateTimeFormatter DT_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter CHART_TIME_FMT =
      DateTimeFormatter.ofPattern("HH:mm:ss");

  private final Consumer<Response> listener = this::handleResponse;

  // Listener cho TabPane selectedItem — chỉ vẽ chart khi tab 3 được chọn
  private ChangeListener<Tab> tabChangeListener;

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

    // --- Khởi tạo TableView cho Tab 1 ---
    setupBidHistoryTable();

    // --- Khởi tạo GridPane specs cho Tab 2 (sẽ populate sau khi có dữ liệu) ---
    // specsGrid được populate động trong buildSpecsGrid()

    // --- Setup Tab change listener (Tab 3 lazy-load chart) ---
    setupTabChangeListener();

    // --- Các logic cũ ---
    watcherEstimate = 40 + Math.abs(sessionId.hashCode() % 180);
    updateViewerCountLabel();

    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);

    Dto.SubscribeRequest sub = new Dto.SubscribeRequest(sessionId);
    SocketClient.getInstance().sendRequest(new Request(ActionType.SUBSCRIBE_AUCTION, sub));
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_ACTIVE_AUCTIONS, null));
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_AUCTION_BIDS, sub));

    startCountdownTimer();

    // Mặc định mở tab đầu tiên
    if (auctionTabPane != null && !auctionTabPane.getTabs().isEmpty()) {
      auctionTabPane.getSelectionModel().select(0);
    }
  }

  // -------------------------------------------------------
  // SETUP TAB 1: BID HISTORY TABLE
  // -------------------------------------------------------

  /**
   * Thiết lập TableView hiển thị lịch sử đặt giá.
   * Sử dụng BidRow POJO để dễ binding với TableColumn.
   */
  private void setupBidHistoryTable() {
    if (bidHistoryTable == null) return;

    // Áp dụng CONSTRAINED_RESIZE_POLICY programmatically thay vì trong FXML
    // để tránh lỗi "Unable to coerce TableView.CONSTRAINED_RESIZE_POLICY to interface Callback"
    bidHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    // Khởi tạo các cột
    timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
    bidderColumn.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
    amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
    typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

    // Binding dữ liệu
    bidHistoryTable.setItems(bidTableData);

    // Style cho hàng dẫn đầu (giá cao nhất) - dùng rowFactory
    bidHistoryTable.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
      @Override
      protected void updateItem(BidRow item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          getStyleClass().remove("bid-leader-row");
          return;
        }
        // Hàng đầu tiên (giá cao nhất) được highlight vàng
        if (getIndex() == 0 && !bidTableData.isEmpty()) {
          if (!getStyleClass().contains("bid-leader-row")) {
            getStyleClass().add("bid-leader-row");
          }
        } else {
          getStyleClass().remove("bid-leader-row");
        }
      }
    });

    bidTableStatusLabel.setText("");
  }

  // =======================================================
  //  DATA CLASS BIDROW dùng cho TableView
  //  (POJO với PropertyValueFactory-compatible getters)
  // =======================================================

  /**
   * POJO đơn giản để hiển thị trên TableView.
   * PropertyValueFactory dùng Java Bean convention:
   * "time" → getTime(), "bidderName" → getBidderName(), v.v.
   */
  public static class BidRow {
    private final String time;
    private final String bidderName;
    private final String amount;
    private final String type;
    private final double rawAmount;

    public BidRow(String time, String bidderName, double amount, String type) {
      this.time = time;
      this.bidderName = bidderName;
      this.rawAmount = amount;
      this.amount = String.format("%,.0f", amount);
      this.type = type;
    }

    public String getTime() { return time; }
    public String getBidderName() { return bidderName; }
    public String getAmount() { return amount; }
    public String getType() { return type; }
    public double getRawAmount() { return rawAmount; }
  }

  // -------------------------------------------------------
  // SETUP TAB 2: PRODUCT SPECS
  // -------------------------------------------------------

  /**
   * Xây dựng GridPane hiển thị thông tin chi tiết sản phẩm (Tab 2).
   * Sử dụng GridPane 2 cột với hgap=30, vgap=15.
   * Cột 1: Icon + Nhãn (căn phải)
   * Cột 2: Giá trị (căn trái) — tự động xuống dòng nếu dài
   * Bổ sung: Tình trạng, Chất liệu, Bảo hành (từ DTO mới)
   * Hàng cuối cùng: Mô tả (columnSpan=2)
   */
  private void buildSpecsGrid(Dto.AuctionCardDto session) {
    if (specsGrid == null || session == null) return;

    specsGrid.getChildren().clear();
    specsGrid.getRowConstraints().clear();

    // --- Định nghĩa danh sách specs với Icon + Label + Value + Style ---
    // Mỗi mảng: {iconLabel, valueText, valueStyleClass (null = default)}
    String[][] specData = {
        {"📦 Tên sản phẩm",        session.itemName() != null ? session.itemName() : "N/A",
         "spec-value spec-value-bold-white"},
        {"📂 Danh mục",            session.itemCategory() != null ? session.itemCategory() : "N/A",
         null},
        {"👤 Người bán",           session.sellerName() != null ? session.sellerName() : "N/A",
         null},
        {"🔧 Tình trạng",          session.conditionStr() != null ? session.conditionStr() : "N/A",
         null},
        {"🧱 Chất liệu",           session.material() != null ? session.material() : "N/A",
         null},
        {"🛡️ Bảo hành",           session.warrantyMonths() > 0
                                     ? session.warrantyMonths() + " tháng"
                                     : "Không bảo hành",
         null},
        {"💰 Giá khởi điểm",       formatMoney(session.basePrice()) + " VND",
         null},
        {"📊 Bước giá tối thiểu",  formatMoney(session.minIncrement()) + " VND",
         null},
        {"💎 Giá hiện tại",        formatMoney(session.currentPrice()) + " VND",
         "spec-value spec-value-emerald"},
    };

    // --- Populate GridPane từ specData ---
    int dataRowCount = specData.length;

    for (int i = 0; i < dataRowCount; i++) {
      int row = i;

      // Không cần RowConstraints cố định, dùng vgap=15 trong FXML

      // ===== CỘT 1: Icon + Label =====
      Label labelNode = new Label(specData[i][0]); // "🔧 Tình trạng"
      labelNode.getStyleClass().add("spec-label");
      labelNode.setMaxWidth(Double.MAX_VALUE);
      GridPane.setConstraints(labelNode, 0, row);

      // ===== CỘT 2: Giá trị =====
      Label valueNode = new Label(specData[i][1]);
      String styleClassStr = specData[i][2];
      if (styleClassStr != null) {
        valueNode.getStyleClass().setAll(styleClassStr.split(" "));
      } else {
        valueNode.getStyleClass().add("spec-value");
      }
      valueNode.setMaxWidth(Double.MAX_VALUE);
      valueNode.setWrapText(true);
      GridPane.setConstraints(valueNode, 1, row);
      GridPane.setFillWidth(valueNode, true);

      specsGrid.getChildren().addAll(labelNode, valueNode);
    }

    // ===== HÀNG CUỐI: Mô tả (columnSpan = 2) =====
    int descRow = dataRowCount;
    String description = session.itemDescription() != null && !session.itemDescription().isBlank()
        ? session.itemDescription() : "Không có mô tả";

    Label descLabel = new Label("📝 Mô tả sản phẩm");
    descLabel.getStyleClass().add("spec-label");
    descLabel.setMaxWidth(Double.MAX_VALUE);
    GridPane.setConstraints(descLabel, 0, descRow);

    Label descValue = new Label(description);
    descValue.getStyleClass().addAll("spec-value", "spec-value-desc");
    descValue.setMaxWidth(Double.MAX_VALUE);
    descValue.setWrapText(true);
    GridPane.setConstraints(descValue, 1, descRow);
    GridPane.setFillWidth(descValue, true);
    GridPane.setColumnSpan(descValue, 2);
    GridPane.setFillWidth(descValue, true);

    specsGrid.getChildren().addAll(descLabel, descValue);
  }

  // -------------------------------------------------------
  // SETUP TAB 3: CHART — LAZY LOAD
  // -------------------------------------------------------

  /**
   * Thiết lập listener cho TabPane.
   * Chỉ vẽ/cập nhật biểu đồ Tab 3 khi người dùng click vào tab đó.
   */
  private void setupTabChangeListener() {
    if (auctionTabPane == null) return;

    tabChangeListener = (observable, oldTab, newTab) -> {
      if (newTab == null) return;
      int newIndex = auctionTabPane.getTabs().indexOf(newTab);
      if (newIndex == 2) {
        // Tab 3 (index 2) — Biểu đồ đấu giá được chọn
        Platform.runLater(this::updateChartData);
      }
    };
    auctionTabPane.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
  }

  /**
   * Cập nhật dữ liệu biểu đồ từ danh sách bids.
   * Gọi khi tab 3 được chọn (lazy loading).
   */
  private void updateChartData() {
    if (priceChart == null || chartPlaceholder == null) return;

    // Ẩn placeholder
    chartPlaceholder.setVisible(false);
    chartPlaceholder.setManaged(false);

    // Xoá dữ liệu cũ
    priceChart.getData().clear();

    if (allBids.isEmpty()) {
      chartPlaceholder.setText("Chưa có dữ liệu đấu giá để hiển thị biểu đồ.");
      chartPlaceholder.setVisible(true);
      chartPlaceholder.setManaged(true);
      return;
    }

    // Tạo series dữ liệu
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    series.setName("Giá đấu");

    // Sắp xếp bids theo thời gian tăng dần (từ cũ đến mới)
    List<Bid> sortedBids = allBids.stream()
        .sorted(Comparator.comparing(Bid::getTimestamp))
        .toList();

    for (Bid bid : sortedBids) {
      String label = bid.getTimestamp().format(CHART_TIME_FMT);
      // Thêm index để tránh trùng label trên trục X
      String xValue = label + " (#" + (sortedBids.indexOf(bid) + 1) + ")";
      series.getData().add(new XYChart.Data<>(xValue, bid.getAmount()));
    }

    priceChart.getData().add(series);

    // Tự động điều chỉnh Y-axis range
    if (!sortedBids.isEmpty()) {
      double minVal = sortedBids.stream().mapToDouble(Bid::getAmount).min().orElse(0);
      double maxVal = sortedBids.stream().mapToDouble(Bid::getAmount).max().orElse(0);
      double padding = Math.max(100000, (maxVal - minVal) * 0.1);
      chartYAxis.setAutoRanging(false);
      chartYAxis.setLowerBound(Math.max(0, minVal - padding));
      chartYAxis.setUpperBound(maxVal + padding);
      chartYAxis.setTickUnit(Math.max(100000, (maxVal - minVal) / 5));
    }
  }

  // -------------------------------------------------------
  // TAB 4: AUTOBID LOGIC
  // -------------------------------------------------------

  @FXML
  private void handleActivateAutobid() {
    if (autobidActive) {
      // Hủy kích hoạt
      deactivateAutobid();
      return;
    }

    // Validate input
    String ceilingRaw = autobidCeilingField != null ? autobidCeilingField.getText().replace(",", "").trim() : "";
    String incrementRaw = autobidIncrementField != null ? autobidIncrementField.getText().replace(",", "").trim() : "";

    if (ceilingRaw.isEmpty()) {
      setAutobidStatus("Vui lòng nhập mức giá tối đa.", true);
      return;
    }
    if (incrementRaw.isEmpty()) {
      setAutobidStatus("Vui lòng nhập bước giá tự động tăng.", true);
      return;
    }

    double ceiling;
    double increment;
    try {
      ceiling = Double.parseDouble(ceilingRaw);
      increment = Double.parseDouble(incrementRaw);
    } catch (NumberFormatException e) {
      setAutobidStatus("Vui lòng nhập số hợp lệ (VD: 5000000 cho 5 triệu).", true);
      return;
    }

    if (ceiling <= currentPrice) {
      setAutobidStatus("Mức giá tối đa phải lớn hơn giá hiện tại (" + formatMoney(currentPrice) + " VND).", true);
      return;
    }
    if (increment < 10000) {
      setAutobidStatus("Bước giá tối thiểu là 10,000 VND.", true);
      return;
    }
    if (ceiling < currentPrice + increment) {
      setAutobidStatus("Mức giá tối đa phải cao hơn giá hiện tại + bước giá.", true);
      return;
    }

    // Kích hoạt AutoBid — gửi request lên server
    activateAutobid(ceiling, increment);
  }

  private void activateAutobid(double ceiling, double increment) {
    Dto.AutoBidConfigRequest config = new Dto.AutoBidConfigRequest(
        sessionId, ceiling, "AGGRESSIVE"
    );
    SocketClient.getInstance().sendRequest(new Request(ActionType.REGISTER_AUTO_BID, config));

    // UI feedback
    Platform.runLater(() -> {
      autobidActive = true;
      if (activateAutobidBtn != null) {
        activateAutobidBtn.setText("⏹ HỦY AUTOBID");
        activateAutobidBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 16 32; -fx-cursor: hand; -fx-min-height: 54;");
      }
      if (autobidCeilingField != null) autobidCeilingField.setDisable(true);
      if (autobidIncrementField != null) autobidIncrementField.setDisable(true);
      setAutobidStatus("✅ AutoBid đã kích hoạt! Hệ thống sẽ tự động đặt giá thay bạn.", false);
    });
  }

  private void deactivateAutobid() {
    // Gửi request hủy
    Dto.AutoBidConfigRequest config = new Dto.AutoBidConfigRequest(
        sessionId, 0, "CONSERVATIVE"
    );
    SocketClient.getInstance().sendRequest(new Request(ActionType.REGISTER_AUTO_BID, config));

    Platform.runLater(() -> {
      autobidActive = false;
      if (activateAutobidBtn != null) {
        activateAutobidBtn.setText("KÍCH HOẠT AUTOBID");
        activateAutobidBtn.setStyle(null); // Reset về CSS
        activateAutobidBtn.getStyleClass().add("autobid-activate-btn");
      }
      if (autobidCeilingField != null) autobidCeilingField.setDisable(false);
      if (autobidIncrementField != null) autobidIncrementField.setDisable(false);
      setAutobidStatus("⏸ AutoBid đã hủy kích hoạt.", false);
    });
  }

  private void setAutobidStatus(String msg, boolean isError) {
    if (autobidStatusLabel == null) return;
    autobidStatusLabel.setText(msg);
    autobidStatusLabel.getStyleClass().remove("autobid-status-label-error");
    if (isError) {
      autobidStatusLabel.getStyleClass().add("autobid-status-label-error");
    }
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
      case REGISTER_AUTO_BID         -> handleAutoBidResponse(response);
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

    // Cache dữ liệu phiên
    currentSessionData = selected;

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

      // Cập nhật Tab 2: Thông tin chi tiết sản phẩm
      buildSpecsGrid(selected);
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
      // Cập nhật cache allBids (cho biểu đồ)
      allBids.clear();
      allBids.addAll(bids);

      // Cập nhật TableView (Tab 1)
      bidTableData.clear();
      if (bids.isEmpty()) {
        if (bidTableStatusLabel != null) {
          bidTableStatusLabel.setText("Chưa có lượt đặt giá nào.");
        }
      } else {
        if (bidTableStatusLabel != null) {
          bidTableStatusLabel.setText("Tổng số: " + bids.size() + " lượt đặt giá");
        }
        bids.forEach(bid -> bidTableData.add(new BidRow(
            bid.getTimestamp().format(DT_FMT),
            bid.getBidderName() != null ? bid.getBidderName() : "Hệ thống",
            bid.getAmount(),
            bid.getBidType() != null ? bid.getBidType().getDisplayName() : "N/A"
        )));
      }

      // Nếu Tab 3 đang được chọn, cập nhật biểu đồ
      int selectedIndex = auctionTabPane != null && auctionTabPane.getSelectionModel().getSelectedItem() != null
          ? auctionTabPane.getTabs().indexOf(auctionTabPane.getSelectionModel().getSelectedItem()) : -1;
      if (selectedIndex == 2) {
        updateChartData();
      }

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

      // Cập nhật bid history
      LocalDateTime ts;
      try {
        ts = (event.timestamp() != null && !event.timestamp().isBlank())
            ? LocalDateTime.parse(event.timestamp()) : LocalDateTime.now();
      } catch (Exception e) {
        ts = LocalDateTime.now();
      }

      // Thêm vào TableView (Tab 1)
      bidTableData.add(0, new BidRow(
          ts.format(DT_FMT),
          event.bidderName(),
          event.amount(),
          "Đặt thủ công"
      ));
      if (bidTableStatusLabel != null) {
        bidTableStatusLabel.setText("Tổng số: " + bidTableData.size() + " lượt đặt giá");
      }

      // Thêm vào cache allBids cho biểu đồ
      Bid newBid = Bid.createManual(event.sessionId(), event.bidderId(), event.bidderName(), event.amount());
      allBids.add(newBid);

      // Nếu Tab 3 đang chọn, cập nhật biểu đồ
      int selectedIndex = auctionTabPane != null && auctionTabPane.getSelectionModel().getSelectedItem() != null
          ? auctionTabPane.getTabs().indexOf(auctionTabPane.getSelectionModel().getSelectedItem()) : -1;
      if (selectedIndex == 2) {
        updateChartData();
      }

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

  private void handleAutoBidResponse(Response response) {
    Platform.runLater(() -> {
      if (response.isSuccess()) {
        if (!autobidActive) {
          setAutobidStatus("✅ AutoBid đã kích hoạt thành công!", false);
        } else {
          setAutobidStatus("✅ Cấu hình AutoBid đã được cập nhật.", false);
        }
      } else {
        setAutobidStatus("❌ Lỗi AutoBid: " + response.getMessage(), true);
        // Rollback UI nếu có lỗi
        if (autobidActive) {
          deactivateAutobid();
        }
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

    // ===== KIỂM TRA PHÂN QUYỀN ĐẶT GIÁ (QUAN TRỌNG) =====
    // Quy tắc chuẩn xác:
    // 1. ADMIN tuyệt đối không được đặt giá
    // 2. CHỦ SỞ HỮU (người đăng) không được tự đặt giá sản phẩm của chính mình
    // 3. BIDDER và SELLER đều được phép đặt giá (nếu không vi phạm 2 điều trên)
    boolean canBid = false;
    String hintText = "";

    if (!isRunning) {
      hintText = "Phiên đang " + ("OPEN".equals(currentStatus) ? "chờ mở" : "kết thúc");
    } else if (!isLoggedIn || me == null) {
      hintText = "Đăng nhập để tham gia đấu giá";
    } else if ("ADMIN".equals(me.role())) {
      hintText = "Tài khoản quản trị không được phép tham gia đấu giá";
    } else {
      // Kiểm tra nếu user là chủ sở hữu của phiên đấu giá này
      String currentUserId = me.id();
      String sessionSellerId = (currentSessionData != null) ? currentSessionData.sellerId() : null;
      boolean isOwner = (sessionSellerId != null && sessionSellerId.equals(currentUserId));

      if (isOwner) {
        hintText = "Bạn không thể tự đấu giá sản phẩm do chính mình đăng bán";
      } else {
        canBid = true;
        double nextMin = currentPrice + minIncrement;
        hintText = "Giá tối thiểu tiếp theo: " + formatMoney(nextMin) + " VND";
      }
    }

    if (submitBidBtn != null) submitBidBtn.setDisable(!canBid);
    if (bidAmountField != null) bidAmountField.setDisable(!canBid);
    setPresetDisable(!canBid);
    if (minBidHintLabel != null) minBidHintLabel.setText(hintText);
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

    // 2. Detect if it's a local file path
    boolean isLocalPath = !urlString.startsWith("http://") && !urlString.startsWith("https://");

    if (isLocalPath) {
      try {
        java.io.File file;
        if (urlString.startsWith("file:/")) {
          javafx.scene.image.Image localImg = new javafx.scene.image.Image(urlString);
          if (!localImg.isError() && localImg.getWidth() > 0) {
            final javafx.scene.image.Image finalImg = localImg;
            Platform.runLater(() -> productImageView.setImage(finalImg));
            return;
          }
        } else {
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
    }
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

    // Remove tab change listener
    if (auctionTabPane != null && tabChangeListener != null) {
      auctionTabPane.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
    }

    SocketClient.getInstance().removeListener(listener);
    unsubscribeCurrentSession();
  }
}