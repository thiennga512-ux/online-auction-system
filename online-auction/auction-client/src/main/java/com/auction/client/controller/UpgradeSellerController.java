package com.auction.client.controller;

import com.auction.client.ClientMain;
import com.auction.client.network.SocketClient;
import com.auction.common.dto.Dto;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class UpgradeSellerController implements LifecycleAwareController {

  @FXML private TextField shopNameField;
  @FXML private TextField citizenIdField;
  @FXML private Label shopNameErrorLabel;
  @FXML private Label citizenIdErrorLabel;
  @FXML private Label formErrorLabel;
  @FXML private Button submitButton;

  private final Consumer<Response> listener = this::handleResponse;

  @FXML
  public void initialize() {
    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);

    shopNameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    citizenIdField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    validateForm();
  }

  @FXML
  private void handleSubmit() {
    if (!validateForm()) {
      return;
    }
    formErrorLabel.setText("");
    Dto.UpgradeToSellerRequest payload = new Dto.UpgradeToSellerRequest(
        shopNameField.getText().trim(), citizenIdField.getText().trim());
    SocketClient.getInstance().sendRequest(new Request(ActionType.UPGRADE_TO_SELLER, payload));
  }

  @FXML
  private void handleBack() {
    ClientMain.getMainController().switchContent("home.fxml");
  }

  private void handleResponse(Response response) {
    if (response.getActionType() != ActionType.UPGRADE_TO_SELLER) {
      return;
    }

    if (response.isSuccess()) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Nâng Cấp Thành Công");
      alert.setHeaderText(null);
      alert.setContentText(response.getMessage() + "\nHệ thống sẽ đăng xuất để bạn đăng nhập lại.");
      alert.showAndWait();
      SocketClient.getInstance().sendRequest(new Request(ActionType.LOGOUT, null));
      return;
    }

    formErrorLabel.setText(response.getMessage());
  }

  private boolean validateForm() {
    String shopName = shopNameField.getText() == null ? "" : shopNameField.getText().trim();
    String citizenId = citizenIdField.getText() == null ? "" : citizenIdField.getText().trim();

    boolean isShopNameValid = shopName.length() >= 3;
    boolean isCitizenIdValid = citizenId.matches("\\d{9,12}");

    shopNameErrorLabel.setText(isShopNameValid ? "" : "Tên cửa hàng tối thiểu 3 ký tự");
    citizenIdErrorLabel.setText(isCitizenIdValid ? "" : "CCCD phải gồm 9-12 chữ số");
    submitButton.setDisable(!(isShopNameValid && isCitizenIdValid));

    return isShopNameValid && isCitizenIdValid;
  }

  @Override
  public void onBeforeHide() {
    SocketClient.getInstance().removeListener(listener);
  }
}
