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

public class AdminLoginController implements LifecycleAwareController {

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

  /**
   * Xử lý đăng nhập Admin.
   * Gửi request với ActionType.ADMIN_LOGIN.
   * Khi đang xử lý, nút "ĐĂNG NHẬP ADMIN" sẽ hiển thị "Đang xác thực hệ thống..." và bị disable.
   */
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
    loginButton.setDisable(true);
    // Hiệu ứng Loading State: đổi text nút
    loginButton.setText("Đang xác thực hệ thống...");

    if (!SocketClient.getInstance().isConnected()) {
        errorLabel.setText("Lỗi: Không thể kết nối tới Server. Vui lòng bật ServerMain trước!");
        errorLabel.setVisible(true);
        loginButton.setDisable(false);
        loginButton.setText("ĐĂNG NHẬP ADMIN");
        return;
    }

    // Tạo payload và gửi request với ActionType.ADMIN_LOGIN
    Dto.LoginRequest payload = new Dto.LoginRequest(email, password);
    Request request = new Request(ActionType.ADMIN_LOGIN, payload);
    SocketClient.getInstance().sendRequest(request);
  }

  /**
   * Quay lại màn hình đăng nhập người dùng (User Login).
   */
  @FXML
  private void goToUserLogin() {
    ClientMain.getMainController().switchContent("login.fxml");
  }

  /**
   * Xử lý response từ Server.
   * Lắng nghe cả ADMIN_LOGIN và LOGIN để bắt trường hợp sai cổng.
   */
  private void handleResponse(Response response) {
    // Chỉ xử lý ADMIN_LOGIN
    if (response.getActionType() != ActionType.ADMIN_LOGIN) {
      return;
    }

    if (response.isSuccess()) {
      Dto.UserProfileResponse profile = response.getDataAs(Dto.UserProfileResponse.class);
      Platform.runLater(() -> {
        SessionManager.getInstance().setCurrentUser(profile);
        // Admin đăng nhập thành công → về Trang chủ (header sẽ hiện nút "Quản trị")
        ClientMain.getMainController().goToHome();
      });
      return;
    }

    // Đăng nhập thất bại → khôi phục trạng thái nút
    Platform.runLater(() -> {
      errorLabel.setText(response.getMessage());
      errorLabel.setVisible(true);
      loginButton.setDisable(false);
      loginButton.setText("ĐĂNG NHẬP ADMIN");
    });
  }

  @Override
  public void onBeforeHide() {
    SocketClient.getInstance().removeListener(listener);
  }
}