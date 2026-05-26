package com.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.network.Request;
import com.auction.network.Response;
import com.network.SocketClient;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class SellerDashboardController implements LifeCycleAwareController {

  // --- Tab 1: Đăng sản phẩm ---
  @FXML
  private GridPane electronicsGrid;
  @FXML
  private GridPane artGrid;
  @FXML
  private GridPane vehicleGrid;
  @FXML
  private ComboBox<String> categoryComboBox;
  @FXML
  private TextField nameField;
  @FXML
  private TextArea descriptionField;
  @FXML
  private TextField basePriceField;
  @FXML
  private TextField minIncrementField;
  @FXML
  private TextField imageUrlField;

  // Electronics
  @FXML
  private TextField brandField;
  @FXML
  private TextField modelField;
  @FXML
  private TextField warrantyField;
  @FXML
  private ComboBox<String> conditionComboBox;

  // Art
  @FXML
  private TextField artistNameField;
  @FXML
  private TextField creationYearField;
  @FXML
  private TextField mediumField;
  @FXML
  private TextField dimensionsField;
  @FXML
  private TextField certificateIdField;
  @FXML
  private ComboBox<String> authenticatedComboBox;

  // Vehicle
  @FXML
  private ComboBox<String> vehicleTypeComboBox;
  @FXML
  private TextField makeField;
  @FXML
  private TextField vehicleModelField;
  @FXML
  private TextField yearField;
  @FXML
  private TextField mileageField;
  @FXML
  private ComboBox<String> fuelTypeComboBox;
  @FXML
  private ComboBox<String> transmissionComboBox;
  @FXML
  private TextField colorField;
  @FXML
  private TextField licensePlateField;
  @FXML
  private ComboBox<String> hasValidRegistryComboBox;

  // --- Tab 2: Sản phẩm của tôi ---
  @FXML
  private TableView<Dto.ItemResponse> myProductsTable;
  @FXML
  private TableColumn<Dto.ItemResponse, String> colId;
  @FXML
  private TableColumn<Dto.ItemResponse, String> colName;
  @FXML
  private TableColumn<Dto.ItemResponse, Double> colBasePrice;
  @FXML
  private TableColumn<Dto.ItemResponse, String> colStatus;

  // --- Tab 3: Tạo phiên đấu giá (DatePicker + Spinner) ---
  @FXML
  private ComboBox<Dto.ItemResponse> itemComboBox;
  @FXML
  private DatePicker startDatePicker;
  @FXML
  private Spinner<Integer> startHourSpinner;
  @FXML
  private Spinner<Integer> startMinSpinner;
  @FXML
  private Label startTimePreviewLabel;
  @FXML
  private DatePicker endDatePicker;
  @FXML
  private Spinner<Integer> endHourSpinner;
  @FXML
  private Spinner<Integer> endMinSpinner;
  @FXML
  private Label endTimePreviewLabel;
  @FXML
  private TextField antiSnipingField;
  @FXML
  private Label auctionFormMessageLabel;

  // --- Tab 4: Phiên đấu giá của tôi ---
  @FXML
  private TableView<Dto.AuctionSessionResponse> myAuctionsTable;
  @FXML
  private TableColumn<Dto.AuctionSessionResponse, String> colAuctionId;
  @FXML
  private TableColumn<Dto.AuctionSessionResponse, String> colAuctionItem;
  @FXML
  private TableColumn<Dto.AuctionSessionResponse, Double> colAuctionPrice;
  @FXML
  private TableColumn<Dto.AuctionSessionResponse, String> colAuctionStatus;
  @FXML
  private TableColumn<Dto.AuctionSessionResponse, String> colAuctionEnd;

  private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final DateTimeFormatter PREVIEW_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

  private final Consumer<Response> listener = this::handleResponse;

  @SuppressWarnings("deprecation")
  @FXML
  public void initialize() {
    // Comboboxes
    categoryComboBox.setItems(FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));
    categoryComboBox.getSelectionModel().select("ELECTRONICS");
    categoryComboBox.valueProperty().addListener((obs, o, n) -> updateCategoryGrids(n));

    conditionComboBox.setItems(FXCollections.observableArrayList("NEW", "LIKE_NEW", "USED", "FOR_PARTS"));
    conditionComboBox.getSelectionModel().selectFirst();
    authenticatedComboBox.setItems(FXCollections.observableArrayList("Có", "Không"));
    authenticatedComboBox.getSelectionModel().selectFirst();
    vehicleTypeComboBox.setItems(FXCollections.observableArrayList("CAR", "MOTORCYCLE", "TRUCK"));
    vehicleTypeComboBox.getSelectionModel().selectFirst();
    fuelTypeComboBox.setItems(FXCollections.observableArrayList("PETROL", "DIESEL", "ELECTRIC", "HYBRID"));
    fuelTypeComboBox.getSelectionModel().selectFirst();
    transmissionComboBox.setItems(FXCollections.observableArrayList("AUTOMATIC", "MANUAL"));
    transmissionComboBox.getSelectionModel().selectFirst();
    hasValidRegistryComboBox.setItems(FXCollections.observableArrayList("Có", "Không"));
    hasValidRegistryComboBox.getSelectionModel().selectFirst();

    // Tables
    if (colId != null)
      colId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().id()));
    if (colName != null)
      colName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name()));
    if (colBasePrice != null)
      colBasePrice
          .setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().startingPrice()));
    if (colStatus != null)
      colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().status()));
    if (colAuctionId != null)
      colAuctionId
          .setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().id().substring(0, 8)));
    if (colAuctionItem != null)
      colAuctionItem.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().itemName()));
    if (colAuctionPrice != null)
      colAuctionPrice
          .setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().currentPrice()));
    if (colAuctionStatus != null)
      colAuctionStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().status()));
    if (colAuctionEnd != null)
      colAuctionEnd.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().endTime()));

    // Set column resize policy programmatically (FXML fx:constant workaround)
    if (myProductsTable != null)
      myProductsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    if (myAuctionsTable != null)
      myAuctionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    if (colBasePrice != null)
      colBasePrice.setCellFactory(tc -> new TableCell<>() {
        protected void updateItem(Double p, boolean empty) {
          super.updateItem(p, empty);
          setText(empty || p == null ? null : String.format("%,.0f đ", p));
        }
      });
    if (colAuctionPrice != null)
      colAuctionPrice.setCellFactory(tc -> new TableCell<>() {
        protected void updateItem(Double p, boolean empty) {
          super.updateItem(p, empty);
          setText(empty || p == null ? null : String.format("%,.0f đ", p));
        }
      });

    if (colStatus != null) {
      colStatus.setCellFactory(tc -> new TableCell<>() {
        @Override
        protected void updateItem(String status, boolean empty) {
          super.updateItem(status, empty);
          if (empty || status == null) {
            setText(null);
            setGraphic(null);
          } else {
            Label label = new Label();
            label.getStyleClass().add("status-badge");
            switch (status) {
              case "AVAILABLE" -> {
                label.setText("Còn hàng");
                label.getStyleClass().add("status-available");
              }
              case "IN_AUCTION" -> {
                label.setText("Đang đấu");
                label.getStyleClass().add("status-running");
              }
              case "SOLD" -> {
                label.setText("Đã bán");
                label.getStyleClass().add("status-sold");
              }
              default -> {
                label.setText(status);
                label.getStyleClass().add("status-unknown");
              }
            }
            setGraphic(label);
            setText(null);
          }
        }
      });
    }

     // Item combobox display
    if (itemComboBox != null)
      itemComboBox.setConverter(new javafx.util.StringConverter<>() {
        public String toString(Dto.ItemResponse i) {
          if (i == null) return "";
          String statusViet = switch (i.status()) {
            case "AVAILABLE" -> "Còn hàng";
            case "IN_AUCTION" -> "Đang đấu";
            case "SOLD" -> "Đã bán";
            default -> i.status();
          };
          return i.name() + " [" + statusViet + "]";
        }

        public Dto.ItemResponse fromString(String s) {
          return null;
        }
      });

    if (colAuctionStatus != null) {
      colAuctionStatus.setCellFactory(tc -> new TableCell<>() {
        @Override
        protected void updateItem(String status, boolean empty) {
          super.updateItem(status, empty);
          if (empty || status == null) {
            setText(null);
            setGraphic(null);
          } else {
            Label label = new Label();
            label.getStyleClass().add("status-badge");
            switch (status) {
              case "PENDING" -> {
                label.setText("Chờ duyệt");
                label.getStyleClass().add("status-pending");
              }
              case "OPEN" -> {
                label.setText("Mở đăng ký");
                label.getStyleClass().add("status-open");
              }
              case "RUNNING" -> {
                label.setText("Đang đấu");
                label.getStyleClass().add("status-running");
              }
              case "FINISHED" -> {
                label.setText("Đã kết thúc");
                label.getStyleClass().add("status-finished");
              }
              case "CANCELLED" -> {
                label.setText("Đã huỷ");
                label.getStyleClass().add("status-cancelled");
              }
              default -> {
                label.setText(status);
                label.getStyleClass().add("status-unknown");
              }
            }
            setGraphic(label);
            setText(null);
          }
        }
      });
    }

    if (colAuctionEnd != null) {
      colAuctionEnd.setCellFactory(tc -> new TableCell<>() {
        @Override
        protected void updateItem(String val, boolean empty) {
          super.updateItem(val, empty);
          if (empty || val == null) {
            setText(null);
          } else {
            try {
              LocalDateTime dt = LocalDateTime.parse(val);
              setText(dt.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            } catch (Exception e) {
              setText(val.replace("T", " "));
            }
          }
        }
      });
    }

    // Spinners giờ / phút
    initSpinners();

    // Giá trị mặc định
    if (antiSnipingField != null)
      antiSnipingField.setText("30");

    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);
    handleRefreshMyProducts();
    handleRefreshMyAuctions();
  }

  // -------------------------------------------------------
  // SPINNER INIT
  // -------------------------------------------------------

  private void initSpinners() {
    LocalDateTime now = LocalDateTime.now();

    if (startHourSpinner != null) {
      startHourSpinner
          .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, now.plusHours(1).getHour()));
      startHourSpinner.valueProperty().addListener((o, a, b) -> updateStartPreview());
    }
    if (startMinSpinner != null) {
      startMinSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
      startMinSpinner.valueProperty().addListener((o, a, b) -> updateStartPreview());
    }
    if (startDatePicker != null) {
      startDatePicker.setValue(now.plusHours(1).toLocalDate());
      startDatePicker.valueProperty().addListener((o, a, b) -> updateStartPreview());
    }

    if (endHourSpinner != null) {
      endHourSpinner
          .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, now.plusHours(3).getHour()));
      endHourSpinner.valueProperty().addListener((o, a, b) -> updateEndPreview());
    }
    if (endMinSpinner != null) {
      endMinSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
      endMinSpinner.valueProperty().addListener((o, a, b) -> updateEndPreview());
    }
    if (endDatePicker != null) {
      endDatePicker.setValue(now.plusHours(3).toLocalDate());
      endDatePicker.valueProperty().addListener((o, a, b) -> updateEndPreview());
    }

    updateStartPreview();
    updateEndPreview();
  }

  private void updateStartPreview() {
    LocalDateTime dt = buildStartTime();
    if (startTimePreviewLabel != null)
      startTimePreviewLabel.setText(dt == null ? "Xem trước: (chọn ngày)" : "Xem trước: " + dt.format(PREVIEW_FMT));
  }

  private void updateEndPreview() {
    LocalDateTime dt = buildEndTime();
    if (endTimePreviewLabel != null)
      endTimePreviewLabel.setText(dt == null ? "Xem trước: (chọn ngày)" : "Xem trước: " + dt.format(PREVIEW_FMT));
  }

  private LocalDateTime buildStartTime() {
    if (startDatePicker == null || startDatePicker.getValue() == null)
      return null;
    LocalDate d = startDatePicker.getValue();
    int h = startHourSpinner != null ? startHourSpinner.getValue() : 0;
    int m = startMinSpinner != null ? startMinSpinner.getValue() : 0;
    return LocalDateTime.of(d, java.time.LocalTime.of(h, m, 0));
  }

  private LocalDateTime buildEndTime() {
    if (endDatePicker == null || endDatePicker.getValue() == null)
      return null;
    LocalDate d = endDatePicker.getValue();
    int h = endHourSpinner != null ? endHourSpinner.getValue() : 0;
    int m = endMinSpinner != null ? endMinSpinner.getValue() : 0;
    return LocalDateTime.of(d, java.time.LocalTime.of(h, m, 0));
  }

  // -------------------------------------------------------
  // PRESET BUTTONS
  // -------------------------------------------------------

  // -------------------------------------------------------
  // PRESET BUTTONS (removed in new FXML — kept as stubs to avoid compile errors)
  // -------------------------------------------------------

  @FXML
  private void handleSetStartNow1h() {
    applyStartOffset(1);
  }

  @FXML
  private void handleSetStartNow3h() {
    applyStartOffset(3);
  }

  @FXML
  private void handleSetEnd2hAfterStart() {
    applyEndOffsetFromStart(2);
  }

  @FXML
  private void handleSetEnd24hAfterStart() {
    applyEndOffsetFromStart(24);
  }

  private void applyStartOffset(int hours) {
    LocalDateTime t = LocalDateTime.now().plusHours(hours).withSecond(0).withNano(0);
    if (startDatePicker != null)
      startDatePicker.setValue(t.toLocalDate());
    if (startHourSpinner != null)
      startHourSpinner.getValueFactory().setValue(t.getHour());
    if (startMinSpinner != null)
      startMinSpinner.getValueFactory().setValue(t.getMinute());
    updateStartPreview();
  }

  private void applyEndOffsetFromStart(int hours) {
    LocalDateTime start = buildStartTime();
    LocalDateTime t = (start != null ? start : LocalDateTime.now()).plusHours(hours);
    if (endDatePicker != null)
      endDatePicker.setValue(t.toLocalDate());
    if (endHourSpinner != null)
      endHourSpinner.getValueFactory().setValue(t.getHour());
    if (endMinSpinner != null)
      endMinSpinner.getValueFactory().setValue(t.getMinute());
    updateEndPreview();
  }

  // -------------------------------------------------------
  // RESPONSE HANDLING
  // -------------------------------------------------------

  private void handleResponse(Response response) {
    if (response == null)
      return;
    Platform.runLater(() -> {
      switch (response.getActionType()) {
        case LIST_ITEM -> handleListItemResponse(response);
        case GET_MY_ITEMS -> handleGetMyItemsResponse(response);
        case CREATE_AUCTION -> handleCreateAuctionResponse(response);
        case GET_MY_AUCTIONS -> handleGetMyAuctionsResponse(response);
        case NEW_AUCTION_BROADCAST, AUCTION_STARTED_BROADCAST, AUCTION_ENDED_BROADCAST -> {
          handleRefreshMyProducts();
          handleRefreshMyAuctions();
        }
        default -> {
        }
      }
    });
  }

  private void handleListItemResponse(Response r) {
    if (r.isSuccess()) {
      showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng sản phẩm thành công!");
      handleClearProductForm();
      handleRefreshMyProducts();
    } else
      showAlert(Alert.AlertType.ERROR, "Lỗi đăng sản phẩm", r.getMessage());
  }

  private void handleGetMyItemsResponse(Response r) {
    if (r.isSuccess()) {
      List<Dto.ItemResponse> items = r.getDataAsList(Dto.ItemResponse.class);
      myProductsTable.setItems(FXCollections.observableArrayList(items));
      if (itemComboBox != null) {
        List<Dto.ItemResponse> availableItems = items.stream()
            .filter(i -> "AVAILABLE".equals(i.status()))
            .toList();
        itemComboBox.setItems(FXCollections.observableArrayList(availableItems));
      }
    } else
      showAlert(Alert.AlertType.ERROR, "Lỗi", r.getMessage());
  }

  private void handleCreateAuctionResponse(Response r) {
    if (auctionFormMessageLabel == null)
      return;
    if (r.isSuccess()) {
      auctionFormMessageLabel.setStyle("-fx-text-fill: #10b981;");
      auctionFormMessageLabel.setText("✅ " + r.getMessage());
      handleRefreshMyAuctions();
      if (itemComboBox != null)
        itemComboBox.getSelectionModel().clearSelection();
      if (antiSnipingField != null)
        antiSnipingField.setText("30");
    } else {
      auctionFormMessageLabel.setStyle("-fx-text-fill: #ef4444;");
      auctionFormMessageLabel.setText("❌ " + r.getMessage());
    }
  }

  private void handleGetMyAuctionsResponse(Response r) {
    if (r.isSuccess() && myAuctionsTable != null)
      myAuctionsTable.setItems(FXCollections.observableArrayList(r.getDataAsList(Dto.AuctionSessionResponse.class)));
    else if (!r.isSuccess())
      showAlert(Alert.AlertType.ERROR, "Lỗi", r.getMessage());
  }

  // -------------------------------------------------------
  // TAB 1: ĐĂNG SẢN PHẨM
  // -------------------------------------------------------

  private void updateCategoryGrids(String cat) {
    if (electronicsGrid != null) {
      electronicsGrid.setVisible("ELECTRONICS".equals(cat));
      electronicsGrid.setManaged("ELECTRONICS".equals(cat));
    }
    if (artGrid != null) {
      artGrid.setVisible("ART".equals(cat));
      artGrid.setManaged("ART".equals(cat));
    }
    if (vehicleGrid != null) {
      vehicleGrid.setVisible("VEHICLE".equals(cat));
      vehicleGrid.setManaged("VEHICLE".equals(cat));
    }
  }

  @FXML
  private void handlePostProduct() {
    try {
      String category = categoryComboBox.getValue();
      String name = nameField.getText().trim();
      String description = descriptionField.getText().trim();
      double basePrice = Double.parseDouble(basePriceField.getText().trim());
      double minIncr = Double.parseDouble(minIncrementField.getText().trim());
      String imageUrl = imageUrlField.getText() != null ? imageUrlField.getText().trim() : "";

      if (name.isEmpty() || description.isEmpty() || imageUrl.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền thông tin chung, bao gồm Image URL.");
        return;
      }
      if (basePrice <= 0 || minIncr <= 0) {
        showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Giá và bước giá phải lớn hơn 0.");
        return;
      }

      String brand = null, model = null, condition = null;
      int warranty = 0;
      String artistName = null, medium = null, certId = null, dims = null;
      int creationYear = 0;
      boolean auth = false;
      String vType = null, make = null, fuel = null, trans = null, color = null, plate = null;
      int yr = 0;
      int mileage = 0;
      boolean registry = false;

      if ("ELECTRONICS".equals(category)) {
        brand = brandField.getText().trim();
        model = modelField.getText().trim();
        warranty = warrantyField.getText().trim().isEmpty() ? 0 : Integer.parseInt(warrantyField.getText().trim());
        condition = conditionComboBox.getValue();
        if (brand.isEmpty() || model.isEmpty()) {
          showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền đủ thông tin Điện tử.");
          return;
        }
      } else if ("ART".equals(category)) {
        artistName = artistNameField.getText().trim();
        creationYear = creationYearField.getText().trim().isEmpty() ? 0
            : Integer.parseInt(creationYearField.getText().trim());
        medium = mediumField.getText().trim();
        dims = dimensionsField.getText().trim();
        certId = certificateIdField.getText().trim();
        auth = "Có".equals(authenticatedComboBox.getValue());
        if (artistName.isEmpty() || medium.isEmpty()) {
          showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền đủ thông tin Nghệ thuật.");
          return;
        }
      } else if ("VEHICLE".equals(category)) {
        vType = vehicleTypeComboBox.getValue();
        make = makeField.getText().trim();
        model = vehicleModelField.getText().trim();
        yr = yearField.getText().trim().isEmpty() ? 0 : Integer.parseInt(yearField.getText().trim());
        mileage = mileageField.getText().trim().isEmpty() ? 0 : Integer.parseInt(mileageField.getText().trim());
        fuel = fuelTypeComboBox.getValue();
        trans = transmissionComboBox.getValue();
        color = colorField.getText().trim();
        plate = licensePlateField.getText().trim();
        registry = "Có".equals(hasValidRegistryComboBox.getValue());
        if (make.isEmpty() || model.isEmpty() || color.isEmpty()) {
          showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền đủ thông tin Phương tiện.");
          return;
        }
      }

      SocketClient.getInstance().sendRequest(new Request(ActionType.LIST_ITEM,
          new Dto.ListItemRequest(name, description, basePrice, minIncr, imageUrl, category,
              brand, model, warranty, condition, artistName, creationYear, medium, auth, certId, dims,
              vType, make, yr, mileage, fuel, trans, color, plate, registry)));
    } catch (NumberFormatException e) {
      showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Giá, bước giá và bảo hành phải là số hợp lệ.");
    }
  }

  @FXML
  private void handleClearProductForm() {
    nameField.clear();
    descriptionField.clear();
    basePriceField.clear();
    minIncrementField.clear();
    if (imageUrlField != null)
      imageUrlField.clear();
    if (brandField != null)
      brandField.clear();
    if (modelField != null)
      modelField.clear();
    if (warrantyField != null)
      warrantyField.clear();
    if (conditionComboBox != null)
      conditionComboBox.getSelectionModel().selectFirst();
    if (artistNameField != null)
      artistNameField.clear();
    if (creationYearField != null)
      creationYearField.clear();
    if (mediumField != null)
      mediumField.clear();
    if (dimensionsField != null)
      dimensionsField.clear();
    if (certificateIdField != null)
      certificateIdField.clear();
    if (authenticatedComboBox != null)
      authenticatedComboBox.getSelectionModel().selectFirst();
    if (vehicleTypeComboBox != null)
      vehicleTypeComboBox.getSelectionModel().selectFirst();
    if (makeField != null)
      makeField.clear();
    if (vehicleModelField != null)
      vehicleModelField.clear();
    if (yearField != null)
      yearField.clear();
    if (mileageField != null)
      mileageField.clear();
    if (fuelTypeComboBox != null)
      fuelTypeComboBox.getSelectionModel().selectFirst();
    if (transmissionComboBox != null)
      transmissionComboBox.getSelectionModel().selectFirst();
    if (colorField != null)
      colorField.clear();
    if (licensePlateField != null)
      licensePlateField.clear();
    if (hasValidRegistryComboBox != null)
      hasValidRegistryComboBox.getSelectionModel().selectFirst();
  }

  // -------------------------------------------------------
  // TAB 2
  // -------------------------------------------------------

  @FXML
  private void handleRefreshMyProducts() {
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_MY_ITEMS, null));
  }

  // -------------------------------------------------------
  // TAB 3: TẠO PHIÊN ĐẤU GIÁ
  // -------------------------------------------------------

  @FXML
  private void handleCreateAuction() {
    if (itemComboBox == null || itemComboBox.getValue() == null) {
      setAuctionMessage("❌ Vui lòng chọn sản phẩm.", false);
      return;
    }
    LocalDateTime startTime = buildStartTime();
    LocalDateTime endTime = buildEndTime();

    if (startTime == null) {
      setAuctionMessage("❌ Vui lòng chọn ngày bắt đầu.", false);
      return;
    }
    if (endTime == null) {
      setAuctionMessage("❌ Vui lòng chọn ngày kết thúc.", false);
      return;
    }
    if (!startTime.isAfter(LocalDateTime.now().plusMinutes(5))) {
      setAuctionMessage(
          "❌ Thời gian bắt đầu phải ít nhất 5 phút từ bây giờ.\nĐang chọn: " + startTime.format(PREVIEW_FMT), false);
      return;
    }
    if (!endTime.isAfter(startTime.plusHours(1))) {
      setAuctionMessage(
          "❌ Thời gian kết thúc phải ít nhất 1 tiếng sau bắt đầu.\nBắt đầu: " + startTime.format(PREVIEW_FMT), false);
      return;
    }

    String antiStr = antiSnipingField != null ? antiSnipingField.getText().trim() : "30";
    int antiSniping;
    try {
      antiSniping = antiStr.isEmpty() ? 30 : Integer.parseInt(antiStr);
    } catch (NumberFormatException e) {
      setAuctionMessage("❌ Anti-Sniping phải là số nguyên.", false);
      return;
    }

    String itemId = itemComboBox.getValue().id();
    SocketClient.getInstance().sendRequest(new Request(ActionType.CREATE_AUCTION,
        new Dto.CreateAuctionRequest(itemId, startTime.format(ISO_FMT), endTime.format(ISO_FMT), antiSniping)));
    setAuctionMessage("⏳ Đang gửi yêu cầu tạo phiên...", true);
  }

  private void setAuctionMessage(String msg, boolean ok) {
    if (auctionFormMessageLabel != null) {
      auctionFormMessageLabel.setStyle(ok ? "-fx-text-fill: #10b981;" : "-fx-text-fill: #ef4444;");
      auctionFormMessageLabel.setText(msg);
    }
  }

  // -------------------------------------------------------
  // TAB 4
  // -------------------------------------------------------

  @FXML
  private void handleRefreshMyAuctions() {
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_MY_AUCTIONS, null));
  }

  @FXML
  private void handleCancelAuction() {
    showAlert(Alert.AlertType.INFORMATION, "Thông báo",
        "Chỉ Admin mới có quyền huỷ phiên đấu giá.\nVui lòng liên hệ quản trị viên nếu cần huỷ.");
  }

  // -------------------------------------------------------
  // HELPER
  // -------------------------------------------------------

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert a = new Alert(type);
    a.setTitle(title);
    a.setHeaderText(null);
    a.setContentText(content);
    a.showAndWait();
  }

  @Override
  public void onBeforeHide() {
    SocketClient.getInstance().removeListener(listener);
  }

  @FXML
  private void handleChooseImage(javafx.event.ActionEvent event) {
    // Hàm này giúp FXML nạp được mà không bị crash
    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
    fileChooser.setTitle("Chọn ảnh sản phẩm");
    java.io.File file = fileChooser.showOpenDialog(null);
    if (file != null) {
      // Gán đường dẫn ảnh vào TextField để gửi lên Server
      imageUrlField.setText(file.toURI().toString());
    }
  }
}
