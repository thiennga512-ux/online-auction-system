package com.Controller;

import java.util.function.Consumer;

import com.ClientMain;
import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.network.Request;
import com.auction.network.Response;
import com.network.SocketClient;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class UpgradeSellerController implements LifeCycleAwareController {

  // ===== Group 1: Thông tin cửa hàng =====
  @FXML
  private TextField shopNameField;
  @FXML
  private TextField phoneField;
  @FXML
  private TextField addressField;

  // ===== Group 2: Xác minh danh tính =====
  @FXML
  private TextField citizenIdField;
  @FXML
  private TextField issueDateField;
  @FXML
  private TextField issuePlaceField;

  // ===== Group 3: Tài khoản nhận tiền =====
  @FXML
  private ComboBox<String> bankComboBox;
  @FXML
  private TextField accountNumberField;
  @FXML
  private TextField accountHolderField;

  // ===== Terms & Error =====
  @FXML
  private CheckBox termsCheckBox;
  @FXML
  private Label formErrorLabel;
  @FXML
  private Button submitButton;

  private final Consumer<Response> listener = this::handleResponse;

  /** Danh sách ngân hàng nội địa Việt Nam */
  private static final String[] BANKS = {
      "Vietcombank (VCB)",
      "VietinBank",
      "BIDV",
      "Agribank",
      "Techcombank",
      "MB Bank",
      "VPBank",
      "ACB",
      "Sacombank",
      "HDBank",
      "SHB",
      "VIB",
      "MSB",
      "TPBank",
      "OCB",
      "Nam A Bank",
      "PVcomBank",
      "SeABank",
      "Eximbank",
      "ABBANK"
  };

  @FXML
  public void initialize() {
    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);

    // Nạp danh sách ngân hàng vào ComboBox
    bankComboBox.getItems().addAll(BANKS);

    // Thêm listeners validation
    shopNameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    citizenIdField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    phoneField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    accountNumberField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    accountHolderField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    bankComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateForm());
    termsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> validateForm());

    // Các trường không bắt buộc cũng validate để đồng bộ
    issueDateField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    issuePlaceField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    addressField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());

    validateForm();
  }

  /**
   * Gửi yêu cầu nâng cấp lên Seller với các thông tin bắt buộc.
   * (Các trường bổ sung như bank, account... sẽ được lưu client-side
   * và có thể mở rộng server sau này)
   */
  @FXML
  private void handleSubmit() {
    if (!validateForm()) {
      return;
    }

    formErrorLabel.setText("");
    formErrorLabel.setVisible(false);

    // Server hiện tại chỉ nhận shopName + citizenId
    // Các trường mở rộng (phone, address, bank...) sẽ được
    // lưu ở client hoặc mở rộng server sau
    Dto.UpgradeToSellerRequest payload = new Dto.UpgradeToSellerRequest(
        shopNameField.getText().trim(),
        citizenIdField.getText().trim());

    submitButton.setDisable(true);
    submitButton.setText("ĐANG XỬ LÝ...");
    SocketClient.getInstance().sendRequest(new Request(ActionType.UPGRADE_TO_SELLER, payload));
  }

  @FXML
  private void handleBack() {
    ClientMain.getMainController().switchContent("home.fxml");
  }

  @FXML
  private void handleTermsOfService() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Quy chế hoạt động");
    alert.setHeaderText("Quy chế hoạt động Sàn Đấu Giá");
    alert.setContentText(
        "1. Người bán cam kết cung cấp thông tin hàng hóa chính xác, trung thực.\n"
            + "2. Mọi giao dịch đấu giá có tính pháp lý và ràng buộc.\n"
            + "3. Sàn không chịu trách nhiệm về chất lượng hàng hóa sau khi giao dịch.\n"
            + "4. Người bán phải tuân thủ quy định về đấu giá trực tuyến.");
    alert.showAndWait();
  }

  @FXML
  private void handlePrivacyPolicy() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Chính sách bảo mật");
    alert.setHeaderText("Điều khoản bảo mật thông tin");
    alert.setContentText(
        "Thông tin cá nhân của bạn sẽ được bảo mật tuyệt đối.\n"
            + "Chúng tôi cam kết không chia sẻ dữ liệu cho bên thứ ba\n"
            + "khi chưa có sự đồng ý của bạn.");
    alert.showAndWait();
  }

  /**
   * Xử lý phản hồi từ server.
   */
  private void handleResponse(Response response) {
    if (response.getActionType() != ActionType.UPGRADE_TO_SELLER) {
      return;
    }

    submitButton.setDisable(false);
    submitButton.setText("ĐĂNG KÝ MỞ KÊNH");

    if (response.isSuccess()) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("🎉 Nâng Cấp Thành Công");
      alert.setHeaderText(null);
      alert.setContentText(response.getMessage() + "\n\nHệ thống sẽ đăng xuất để bạn đăng nhập lại với quyền Seller.");
      alert.showAndWait();

      SocketClient.getInstance().sendRequest(new Request(ActionType.LOGOUT, null));
      return;
    }

    formErrorLabel.setText(response.getMessage());
    formErrorLabel.setVisible(true);
  }

  /**
   * Validate toàn bộ form trước khi submit.
   * Nếu có lỗi, các ô sẽ hiển thị viền đỏ (qua CSS) và nút submit bị vô hiệu.
   */
  private boolean validateForm() {
    String shopName = shopNameField.getText() == null ? "" : shopNameField.getText().trim();
    String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
    String address = addressField.getText() == null ? "" : addressField.getText().trim();
    String citizenId = citizenIdField.getText() == null ? "" : citizenIdField.getText().trim();
    String bank = bankComboBox.getValue();
    String accountNumber = accountNumberField.getText() == null ? "" : accountNumberField.getText().trim();
    String accountHolder = accountHolderField.getText() == null ? "" : accountHolderField.getText().trim();

    // Validate từng trường (dùng CSS class để báo lỗi)
    boolean isShopNameValid = shopName.length() >= 3;
    boolean isPhoneValid = phone.matches("^0[0-9]{9,10}$");
    boolean isAddressValid = address.length() >= 5;
    boolean isCitizenIdValid = citizenId.matches("\\d{9,12}");
    boolean isBankValid = bank != null && !bank.isEmpty();
    boolean isAccountNumberValid = accountNumber.matches("\\d{8,20}");
    boolean isAccountHolderValid = accountHolder.length() >= 5;
    boolean isTermsChecked = termsCheckBox.isSelected();

    // Highlight viền đỏ cho các trường lỗi (dùng styleClass)
    applyFieldError(shopNameField, !isShopNameValid);
    applyFieldError(phoneField, !isPhoneValid);
    applyFieldError(addressField, !isAddressValid);
    applyFieldError(citizenIdField, !isCitizenIdValid);
    applyFieldError(accountNumberField, !isAccountNumberValid);
    applyFieldError(accountHolderField, !isAccountHolderValid);

    // Tổng hợp lỗi
    boolean allValid = isShopNameValid && isPhoneValid && isAddressValid
        && isCitizenIdValid && isBankValid
        && isAccountNumberValid && isAccountHolderValid
        && isTermsChecked;

    submitButton.setDisable(!allValid);

    return allValid;
  }

  /**
   * Thêm/xoá class báo lỗi cho TextField.
   * Khi có lỗi → thêm class "upgrade-field--error" (viền đỏ).
   * Khi không lỗi → xoá class đó.
   */
  private void applyFieldError(TextField field, boolean hasError) {
    if (hasError) {
      if (!field.getStyleClass().contains("upgrade-field--error")) {
        field.getStyleClass().add("upgrade-field--error");
      }
    } else {
      field.getStyleClass().remove("upgrade-field--error");
    }
  }

  @Override
  public void onBeforeHide() {
    SocketClient.getInstance().removeListener(listener);
  }
}
