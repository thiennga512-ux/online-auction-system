package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.common.dto.Dto;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * ============================================================
 * SellerDashboardController — Kênh Người Bán
 * ============================================================
 *
 * Chức năng:
 *   - Tab 1: Đăng sản phẩm mới (Electronics)
 *   - Tab 2: Xem danh sách sản phẩm của mình
 *   - Tab 3: Tạo phiên đấu giá từ sản phẩm đã có
 *   - Tab 4: Xem danh sách phiên đấu giá của mình
 *
 * Implements LifecycleAwareController để gỡ listener khi rời màn hình.
 * ============================================================
 */
public class SellerDashboardController implements LifecycleAwareController {

  // --- Category Grids ---
  @FXML private GridPane electronicsGrid;
  @FXML private GridPane artGrid;
  @FXML private GridPane vehicleGrid;
  @FXML private ComboBox<String> categoryComboBox;

  // --- Tab 1: Đăng sản phẩm ---
  @FXML private TextField nameField;
  @FXML private TextArea descriptionField;
  @FXML private TextField basePriceField;
  @FXML private TextField minIncrementField;
  @FXML private TextField imageUrlField;

  // Electronics
  @FXML private TextField brandField;
  @FXML private TextField modelField;
  @FXML private TextField warrantyField;
  @FXML private ComboBox<String> conditionComboBox;

  // Art
  @FXML private TextField artistNameField;
  @FXML private TextField creationYearField;
  @FXML private TextField mediumField;
  @FXML private TextField dimensionsField;
  @FXML private TextField certificateIdField;
  @FXML private ComboBox<String> authenticatedComboBox;

  // Vehicle
  @FXML private ComboBox<String> vehicleTypeComboBox;
  @FXML private TextField makeField;
  @FXML private TextField vehicleModelField;
  @FXML private TextField yearField;
  @FXML private TextField mileageField;
  @FXML private ComboBox<String> fuelTypeComboBox;
  @FXML private ComboBox<String> transmissionComboBox;
  @FXML private TextField colorField;
  @FXML private TextField licensePlateField;
  @FXML private ComboBox<String> hasValidRegistryComboBox;

  // --- Tab 2: Sản phẩm của tôi ---
  @FXML private TableView<Dto.ItemResponse> myProductsTable;
  @FXML private TableColumn<Dto.ItemResponse, String> colId;
  @FXML private TableColumn<Dto.ItemResponse, String> colName;
  @FXML private TableColumn<Dto.ItemResponse, Double> colBasePrice;
  @FXML private TableColumn<Dto.ItemResponse, String> colStatus;

  // --- Tab 3: Tạo phiên đấu giá ---
  @FXML private ComboBox<Dto.ItemResponse> itemComboBox;
  @FXML private TextField startTimeField;
  @FXML private TextField endTimeField;
  @FXML private TextField antiSnipingField;
  @FXML private Label auctionFormMessageLabel;

  // --- Tab 4: Phiên đấu giá của tôi ---
  @FXML private TableView<Dto.AuctionSessionResponse> myAuctionsTable;
  @FXML private TableColumn<Dto.AuctionSessionResponse, String> colAuctionId;
  @FXML private TableColumn<Dto.AuctionSessionResponse, String> colAuctionItem;
  @FXML private TableColumn<Dto.AuctionSessionResponse, Double> colAuctionPrice;
  @FXML private TableColumn<Dto.AuctionSessionResponse, String> colAuctionStatus;
  @FXML private TableColumn<Dto.AuctionSessionResponse, String> colAuctionEnd;

  // Dùng named field để có thể removeListener sau
  private final Consumer<Response> listener = this::handleResponse;

  @FXML
  public void initialize() {
    categoryComboBox.setItems(FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));
    categoryComboBox.getSelectionModel().select("ELECTRONICS");
    categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCategoryGrids(newVal));

    conditionComboBox.setItems(FXCollections.observableArrayList(
        "NEW", "LIKE_NEW", "USED", "FOR_PARTS"
    ));
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

    // Table columns binding cho myProductsTable — dùng lambda vì Java Records không có getXxx()
    if (colId != null) colId.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(data.getValue().id()));
    if (colName != null) colName.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(data.getValue().name()));
    if (colBasePrice != null) colBasePrice.setCellValueFactory(data ->
        new javafx.beans.property.SimpleObjectProperty<>(data.getValue().basePrice()));
    if (colStatus != null) colStatus.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(data.getValue().status()));

    // Table columns binding cho myAuctionsTable
    if (colAuctionId != null) colAuctionId.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(data.getValue().id().substring(0, 8)));
    if (colAuctionItem != null) colAuctionItem.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(data.getValue().itemName()));
    if (colAuctionPrice != null) colAuctionPrice.setCellValueFactory(data ->
        new javafx.beans.property.SimpleObjectProperty<>(data.getValue().currentPrice()));
    if (colAuctionStatus != null) colAuctionStatus.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(data.getValue().status()));
    if (colAuctionEnd != null) colAuctionEnd.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(data.getValue().endTime()));

    // ComboBox hiển thị tên sản phẩm
    if (itemComboBox != null) {
      itemComboBox.setConverter(new javafx.util.StringConverter<Dto.ItemResponse>() {
        @Override public String toString(Dto.ItemResponse item) {
          return item == null ? "" : item.name() + " [" + item.status() + "]";
        }
        @Override public Dto.ItemResponse fromString(String s) { return null; }
      });
    }

    // Đặt placeholder thời gian
    if (startTimeField != null) startTimeField.setPromptText("VD: " + LocalDateTime.now().plusHours(1).withSecond(0).withNano(0).toString());
    if (endTimeField != null) endTimeField.setPromptText("VD: " + LocalDateTime.now().plusHours(3).withSecond(0).withNano(0).toString());
    if (antiSnipingField != null) antiSnipingField.setText("30");

    // Đăng ký listener (một lần duy nhất, sẽ gỡ bỏ khi onBeforeHide)
    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);

    // Auto-load dữ liệu
    handleRefreshMyProducts();
    handleRefreshMyAuctions();

    // Format prices
    if (colBasePrice != null) {
      colBasePrice.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
        @Override
        protected void updateItem(Double price, boolean empty) {
          super.updateItem(price, empty);
          if (empty || price == null) setText(null);
          else setText(String.format("%,.0f đ", price));
        }
      });
    }

    if (colAuctionPrice != null) {
      colAuctionPrice.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
        @Override
        protected void updateItem(Double price, boolean empty) {
          super.updateItem(price, empty);
          if (empty || price == null) setText(null);
          else setText(String.format("%,.0f đ", price));
        }
      });
    }
  }

  private void updateCategoryGrids(String category) {
    if (electronicsGrid != null) {
      electronicsGrid.setVisible("ELECTRONICS".equals(category));
      electronicsGrid.setManaged("ELECTRONICS".equals(category));
    }
    if (artGrid != null) {
      artGrid.setVisible("ART".equals(category));
      artGrid.setManaged("ART".equals(category));
    }
    if (vehicleGrid != null) {
      vehicleGrid.setVisible("VEHICLE".equals(category));
      vehicleGrid.setManaged("VEHICLE".equals(category));
    }
  }

  // -------------------------------------------------------
  // LISTENER XỬ LÝ PHẢN HỒI
  // -------------------------------------------------------

  private void handleResponse(Response response) {
    if (response == null) return;
    Platform.runLater(() -> {
      switch (response.getActionType()) {
        case LIST_ITEM -> handleListItemResponse(response);
        case GET_MY_ITEMS -> handleGetMyItemsResponse(response);
        case CREATE_AUCTION -> handleCreateAuctionResponse(response);
        case GET_MY_AUCTIONS -> handleGetMyAuctionsResponse(response);
        case CANCEL_AUCTION -> handleCancelAuctionResponse(response);
        case NEW_AUCTION_BROADCAST, AUCTION_STARTED_BROADCAST, AUCTION_ENDED_BROADCAST -> {
          handleRefreshMyProducts();
          handleRefreshMyAuctions();
        }
        default -> { /* ignore */ }
      }
    });
  }

  private void handleListItemResponse(Response response) {
    if (response.isSuccess()) {
      showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng sản phẩm thành công!");
      handleClearProductForm();
      handleRefreshMyProducts();
    } else {
      showAlert(Alert.AlertType.ERROR, "Lỗi đăng sản phẩm", response.getMessage());
    }
  }

  private void handleGetMyItemsResponse(Response response) {
    if (response.isSuccess()) {
      List<Dto.ItemResponse> items = response.getDataAsList(Dto.ItemResponse.class);
      myProductsTable.setItems(FXCollections.observableArrayList(items));
      // Cập nhật ComboBox chọn item cho tab Tạo Phiên
      if (itemComboBox != null) {
        itemComboBox.setItems(FXCollections.observableArrayList(items));
      }
    } else {
      showAlert(Alert.AlertType.ERROR, "Lỗi", response.getMessage());
    }
  }

  private void handleCreateAuctionResponse(Response response) {
    if (auctionFormMessageLabel != null) {
      if (response.isSuccess()) {
        auctionFormMessageLabel.setStyle("-fx-text-fill: #10b981;");
        auctionFormMessageLabel.setText("✅ " + response.getMessage());
        handleRefreshMyAuctions();
        // Xóa form
        if (itemComboBox != null) itemComboBox.getSelectionModel().clearSelection();
        if (antiSnipingField != null) antiSnipingField.setText("30");
      } else {
        auctionFormMessageLabel.setStyle("-fx-text-fill: #ef4444;");
        auctionFormMessageLabel.setText("❌ " + response.getMessage());
      }
    }
  }

  private void handleGetMyAuctionsResponse(Response response) {
    if (response.isSuccess() && myAuctionsTable != null) {
      List<Dto.AuctionSessionResponse> sessions = response.getDataAsList(Dto.AuctionSessionResponse.class);
      myAuctionsTable.setItems(FXCollections.observableArrayList(sessions));
    } else if (!response.isSuccess()) {
      showAlert(Alert.AlertType.ERROR, "Lỗi", response.getMessage());
    }
  }

  // -------------------------------------------------------
  // TAB 1: ĐĂNG SẢN PHẨM
  // -------------------------------------------------------

  @FXML
  private void handlePostProduct() {
    try {
      String category = categoryComboBox.getValue();
      String name = nameField.getText().trim();
      String description = descriptionField.getText().trim();
      double basePrice = Double.parseDouble(basePriceField.getText().trim());
      double minIncrement = Double.parseDouble(minIncrementField.getText().trim());
      String imageUrl = imageUrlField.getText() != null ? imageUrlField.getText().trim() : "";

      if (name.isEmpty() || description.isEmpty() || imageUrl.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền thông tin chung, bao gồm cả Image URL.");
        return;
      }
      if (basePrice <= 0 || minIncrement <= 0) {
        showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Giá và bước giá phải lớn hơn 0.");
        return;
      }

      String brand = null, model = null, condition = null; int warranty = 0;
      String artistName = null, medium = null, certificateId = null, dimensions = null; int creationYear = 0; boolean authenticated = false;
      String vehicleType = null, make = null, fuelType = null, transmission = null, color = null, licensePlate = null; int year = 0; double mileage = 0; boolean hasValidRegistry = false;

      if ("ELECTRONICS".equals(category)) {
          brand = brandField.getText().trim();
          model = modelField.getText().trim();
          warranty = warrantyField.getText().trim().isEmpty() ? 0 : Integer.parseInt(warrantyField.getText().trim());
          condition = conditionComboBox.getValue();
          if (brand.isEmpty() || model.isEmpty()) {
              showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền đủ thông tin Điện tử."); return;
          }
      } else if ("ART".equals(category)) {
          artistName = artistNameField.getText().trim();
          creationYear = creationYearField.getText().trim().isEmpty() ? 0 : Integer.parseInt(creationYearField.getText().trim());
          medium = mediumField.getText().trim();
          dimensions = dimensionsField.getText().trim();
          certificateId = certificateIdField.getText().trim();
          authenticated = "Có".equals(authenticatedComboBox.getValue());
          if (artistName.isEmpty() || medium.isEmpty()) {
              showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền đủ thông tin Nghệ thuật."); return;
          }
      } else if ("VEHICLE".equals(category)) {
          vehicleType = vehicleTypeComboBox.getValue();
          make = makeField.getText().trim();
          model = vehicleModelField.getText().trim();
          year = yearField.getText().trim().isEmpty() ? 0 : Integer.parseInt(yearField.getText().trim());
          mileage = mileageField.getText().trim().isEmpty() ? 0 : Double.parseDouble(mileageField.getText().trim());
          fuelType = fuelTypeComboBox.getValue();
          transmission = transmissionComboBox.getValue();
          color = colorField.getText().trim();
          licensePlate = licensePlateField.getText().trim();
          hasValidRegistry = "Có".equals(hasValidRegistryComboBox.getValue());
          if (make.isEmpty() || model.isEmpty() || color.isEmpty()) {
              showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền đủ thông tin Phương tiện."); return;
          }
      }

      Dto.ListItemRequest requestPayload = new Dto.ListItemRequest(
          name, description, basePrice, minIncrement, category, imageUrl,
          brand, model, warranty, condition,
          artistName, creationYear, medium, authenticated, certificateId, dimensions,
          vehicleType, make, year, mileage, fuelType, transmission, color, licensePlate, hasValidRegistry
      );
      SocketClient.getInstance().sendRequest(new Request(ActionType.LIST_ITEM, requestPayload));

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
    if (imageUrlField != null) imageUrlField.clear();

    if (brandField != null) brandField.clear();
    if (modelField != null) modelField.clear();
    if (warrantyField != null) warrantyField.clear();
    if (conditionComboBox != null) conditionComboBox.getSelectionModel().selectFirst();

    if (artistNameField != null) artistNameField.clear();
    if (creationYearField != null) creationYearField.clear();
    if (mediumField != null) mediumField.clear();
    if (dimensionsField != null) dimensionsField.clear();
    if (certificateIdField != null) certificateIdField.clear();
    if (authenticatedComboBox != null) authenticatedComboBox.getSelectionModel().selectFirst();

    if (vehicleTypeComboBox != null) vehicleTypeComboBox.getSelectionModel().selectFirst();
    if (makeField != null) makeField.clear();
    if (vehicleModelField != null) vehicleModelField.clear();
    if (yearField != null) yearField.clear();
    if (mileageField != null) mileageField.clear();
    if (fuelTypeComboBox != null) fuelTypeComboBox.getSelectionModel().selectFirst();
    if (transmissionComboBox != null) transmissionComboBox.getSelectionModel().selectFirst();
    if (colorField != null) colorField.clear();
    if (licensePlateField != null) licensePlateField.clear();
    if (hasValidRegistryComboBox != null) hasValidRegistryComboBox.getSelectionModel().selectFirst();
  }

  // -------------------------------------------------------
  // TAB 2: SẢN PHẨM CỦA TÔI
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
    String itemId = itemComboBox.getValue().id();
    String startTimeStr = startTimeField.getText().trim();
    String endTimeStr = endTimeField.getText().trim();
    String antiSnipingStr = antiSnipingField.getText().trim();

    if (startTimeStr.isEmpty() || endTimeStr.isEmpty()) {
      setAuctionMessage("❌ Vui lòng nhập thời gian bắt đầu và kết thúc.", false);
      return;
    }

    try {
      // Validate parse
      LocalDateTime.parse(startTimeStr);
      LocalDateTime.parse(endTimeStr);
      int antiSniping = antiSnipingStr.isEmpty() ? 30 : Integer.parseInt(antiSnipingStr);

      Dto.CreateAuctionRequest payload = new Dto.CreateAuctionRequest(
          itemId, startTimeStr, endTimeStr, antiSniping
      );
      SocketClient.getInstance().sendRequest(new Request(ActionType.CREATE_AUCTION, payload));
      setAuctionMessage("⏳ Đang gửi yêu cầu tạo phiên...", true);

    } catch (Exception e) {
      setAuctionMessage("❌ Thời gian không đúng định dạng (ISO: yyyy-MM-ddTHH:mm:ss). Ví dụ: " +
          LocalDateTime.now().plusHours(1).withSecond(0).withNano(0), false);
    }
  }

  private void setAuctionMessage(String msg, boolean isSuccess) {
    if (auctionFormMessageLabel != null) {
      auctionFormMessageLabel.setStyle(isSuccess ? "-fx-text-fill: #10b981;" : "-fx-text-fill: #ef4444;");
      auctionFormMessageLabel.setText(msg);
    }
  }

  // -------------------------------------------------------
  // TAB 4: PHIÊN ĐẤU GIÁ CỦA TÔI
  // -------------------------------------------------------

  @FXML
  private void handleRefreshMyAuctions() {
    SocketClient.getInstance().sendRequest(new Request(ActionType.GET_MY_AUCTIONS, null));
  }

  @FXML
  private void handleCancelAuction() {
    if (myAuctionsTable == null) return;
    Dto.AuctionSessionResponse selected = myAuctionsTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showAlert(Alert.AlertType.WARNING, "Chưa chọn phiên", "Vui lòng chọn một phiên đấu giá trong bảng trước khi huỷ.");
      return;
    }
    String status = selected.status();
    if (!("PENDING".equals(status) || "OPEN".equals(status) || "RUNNING".equals(status))) {
      showAlert(Alert.AlertType.WARNING, "Không thể huỷ",
          "Phiên đang ở trạng thái '" + status + "' — chỉ có thể huỷ phiên PENDING, OPEN hoặc RUNNING.");
      return;
    }
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận huỷ phiên");
    confirm.setHeaderText(null);
    confirm.setContentText("Bạn có chắc muốn huỷ phiên đấu giá '" + selected.itemName() + "' không?");
    Optional<ButtonType> result = confirm.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      SocketClient.getInstance().sendRequest(
          new Request(ActionType.CANCEL_AUCTION, new Dto.CancelAuctionRequest(selected.id()))
      );
    }
  }

  private void handleCancelAuctionResponse(Response response) {
    if (response.isSuccess()) {
      showAlert(Alert.AlertType.INFORMATION, "Đã huỷ", response.getMessage());
      handleRefreshMyAuctions();
    } else {
      showAlert(Alert.AlertType.ERROR, "Lỗi huỷ phiên", response.getMessage());
    }
  }

  // -------------------------------------------------------
  // HELPER
  // -------------------------------------------------------

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  @Override
  public void onBeforeHide() {
    // Gỡ listener để tránh memory leak và xử lý sự kiện khi màn hình đã ẩn
    SocketClient.getInstance().removeListener(listener);
  }
}
