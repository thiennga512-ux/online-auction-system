package com.auction.client.controller;

import com.auction.client.ClientMain;
import com.auction.client.network.SocketClient;
import com.auction.common.dto.Dto;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.client.util.SessionManager;
import com.auction.common.network.Response;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements LifecycleAwareController {

  @FXML
  private TextField emailField;

  @FXML
  private PasswordField passwordField;

  @FXML
  private Label errorLabel;

  @FXML
  private Button loginButton;
  private final Consumer<Response> listener = this::handleResponse;

  @FXML
  public void initialize() {
    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);
  }

  @FXML
  private void handleLogin() {
    String email = emailField.getText().trim();
    String password = passwordField.getText();

    if (email.isEmpty() || password.isEmpty()) {
      errorLabel.setText("Vui lòng nhập đầy đủ Email và Mật khẩu!");
      errorLabel.setVisible(true);
      return;
    }

    errorLabel.setVisible(false);
    loginButton.setDisable(true); // Tránh click nhiều lần

    if (!SocketClient.getInstance().isConnected()) {
        errorLabel.setText("Lỗi: Không thể kết nối tới Server. Vui lòng bật ServerMain trước!");
        errorLabel.setVisible(true);
        loginButton.setDisable(false);
        return;
    }

    // Tạo payload và gửi request
    Dto.LoginRequest payload = new Dto.LoginRequest(email, password);
    Request request = new Request(ActionType.LOGIN, payload);
    SocketClient.getInstance().sendRequest(request);
  }

  @FXML
  private void goToRegister() {
    ClientMain.getMainController().switchContent("register.fxml");
  }

  @FXML
  private void goToAdminLogin() {
    ClientMain.getMainController().switchContent("admin_login.fxml");
  }

  private void handleResponse(Response response) {
    if (response.getActionType() != ActionType.LOGIN) {
      return;
    }

    if (response.isSuccess()) {
      Dto.UserProfileResponse profile = response.getDataAs(Dto.UserProfileResponse.class);
      Platform.runLater(() -> {
        SessionManager.getInstance().setCurrentUser(profile);
        // Mọi role (Bidder, Seller, Admin) đều về Trang chủ và highlight nút Trang chủ
        ClientMain.getMainController().goToHome();
      });
      return;
    }

    Platform.runLater(() -> {
      errorLabel.setText(response.getMessage());
      errorLabel.setVisible(true);
      loginButton.setDisable(false);
    });
  }

  @Override
  public void onBeforeHide() {
    SocketClient.getInstance().removeListener(listener);
  }
}
