package com.auction.client.controller;

import com.auction.client.ClientMain;
import com.auction.client.network.SocketClient;
import com.auction.common.dto.Dto;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.function.Consumer;

public class RegisterController implements LifecycleAwareController {

  @FXML private TextField fullNameField;
  @FXML private TextField usernameField;
  @FXML private TextField emailField;
  @FXML private PasswordField passwordField;
  @FXML private PasswordField confirmPasswordField;
  @FXML private ComboBox<String> genderCombo;
  @FXML private DatePicker dobPicker;
  @FXML private Label errorLabel;
  @FXML private Button registerButton;

  /**
   * Khai báo listener ở cấp field (không phải trong initialize) để đảm bảo
   * luôn là cùng một object reference → removeListener hoạt động đúng.
   */
  private final Consumer<Response> responseListener = response -> {
    if (response.getActionType() == ActionType.REGISTER_USER) {
      Platform.runLater(() -> handleRegisterUserResponse(response));
    }
  };

  @FXML
  public void initialize() {
    // Luôn gỡ trước khi thêm → tránh tích lũy listener khi mở lại trang
    SocketClient.getInstance().removeListener(responseListener);
    SocketClient.getInstance().addListener(responseListener);
  }

  /** Xử lý phản hồi đăng ký từ Server */
  private void handleRegisterUserResponse(Response response) {
    if (response.isSuccess()) {
      showSuccess("Đăng ký thành công! Đang chuyển về Đăng nhập...");
      // Gỡ listener ngay lập tức trước khi delay để không nhận response lạc
      SocketClient.getInstance().removeListener(responseListener);
      new Thread(() -> {
        try {
          Thread.sleep(1500);
          Platform.runLater(this::goToLogin);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }).start();
    } else {
      showError(response.getMessage());
      // Mở khoá nút để user có thể thử lại
      registerButton.setDisable(false);
    }
  }

  @FXML
  private void handleRegister() {
    errorLabel.setVisible(false);

    String fullName = fullNameField.getText().trim();
    String username = usernameField.getText().trim();
    String email = emailField.getText().trim();
    String pass = passwordField.getText();
    String confirmPass = confirmPasswordField.getText();

    // Validate từng trường
    if (fullName.isEmpty()) {
      showError("Vui lòng nhập Họ và Tên!");
      fullNameField.requestFocus();
      return;
    }
    if (username.isEmpty()) {
      showError("Vui lòng nhập Tên hiển thị!");
      usernameField.requestFocus();
      return;
    }
    if (email.isEmpty()) {
      showError("Vui lòng nhập Email!");
      emailField.requestFocus();
      return;
    }
    if (!email.contains("@") || !email.contains(".")) {
      showError("Email không hợp lệ! Ví dụ: example@gmail.com");
      emailField.requestFocus();
      return;
    }
    if (pass.isEmpty()) {
      showError("Vui lòng nhập Mật khẩu!");
      passwordField.requestFocus();
      return;
    }
    if (pass.length() < 6) {
      showError("Mật khẩu phải có ít nhất 6 ký tự!");
      passwordField.requestFocus();
      return;
    }
    if (confirmPass.isEmpty()) {
      showError("Vui lòng nhập lại Mật khẩu xác nhận!");
      confirmPasswordField.requestFocus();
      return;
    }
    if (!pass.equals(confirmPass)) {
      showError("Mật khẩu xác nhận không khớp!");
      confirmPasswordField.requestFocus();
      return;
    }
    if (genderCombo.getValue() == null || genderCombo.getValue().isEmpty()) {
      showError("Vui lòng chọn Giới tính!");
      genderCombo.requestFocus();
      return;
    }
    if (dobPicker.getValue() == null) {
      showError("Vui lòng chọn Ngày sinh!");
      dobPicker.requestFocus();
      return;
    }

    // Khoá nút & gửi request
    registerButton.setDisable(true);
    showSuccess("Đang xử lý đăng ký...");

    Dto.RegisterUserRequest payload = new Dto.RegisterUserRequest(
        fullName, username, email, pass,
        genderCombo.getValue(),
        dobPicker.getValue().toString()
    );
    SocketClient.getInstance().sendRequest(new Request(ActionType.REGISTER_USER, payload));
  }

  private void showError(String msg) {
    errorLabel.setStyle("-fx-text-fill: #ef4444;");
    errorLabel.setText(msg);
    errorLabel.setVisible(true);
  }

  private void showSuccess(String msg) {
    errorLabel.setStyle("-fx-text-fill: #10b981;");
    errorLabel.setText(msg);
    errorLabel.setVisible(true);
  }

  @FXML
  private void goToLogin() {
    // Gỡ listener khi chuyển trang thủ công
    SocketClient.getInstance().removeListener(responseListener);
    ClientMain.getMainController().switchContent("login.fxml");
  }

  @Override
  public void onBeforeHide() {
    // Được gọi bởi MainController.switchContent() khi trang bị ẩn
    SocketClient.getInstance().removeListener(responseListener);
    // Mở khoá nút phòng trường hợp đang chờ response
    if (registerButton != null) {
      registerButton.setDisable(false);
    }
  }
}
