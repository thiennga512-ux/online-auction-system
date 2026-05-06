package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    // 1. Khai báo các thành phần giao diện bằng fx:id
    @FXML private TextField txtRegUsername;
    @FXML private TextField txtRegEmail;
    @FXML private PasswordField txtRegPassword;
    @FXML private PasswordField txtRegConfirmPassword;

    // 2. Hàm xử lý khi nhấn nút ĐĂNG KÝ
    @FXML
    public void handleRegisterAction(ActionEvent event) {
        String username = txtRegUsername.getText();
        String email = txtRegEmail.getText();
        String password = txtRegPassword.getText();
        String confirmPassword = txtRegConfirmPassword.getText();

        // Kiểm tra cơ bản (Validation)
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Lỗi", "Mật khẩu nhập lại không khớp!");
            return;
        }

        // Nếu mọi thứ ổn, in ra console (Sau này bạn sẽ viết code lưu vào Database ở đây)
        System.out.println("Đăng ký thành công:");
        System.out.println("Username: " + username);
        System.out.println("Email: " + email);

        showAlert("Thành công", "Chúc mừng " + username + " đã đăng ký tài khoản thành công!");
    }

    // 3. Hàm chuyển về màn hình Đăng nhập khi nhấn Hyperlink
    @FXML
    public void switchToLogin(ActionEvent event) throws IOException {
        // Tải file Login.fxml
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));

        // Lấy Stage hiện tại từ sự kiện (event)
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        stage.setTitle("Hệ Thống Đấu Giá - Đăng Nhập");

        // Tạo Scene mới với file Login.fxml và hiển thị
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // Hàm tiện ích để hiện thông báo (Alert)
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
