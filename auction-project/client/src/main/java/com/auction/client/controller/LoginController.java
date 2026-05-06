package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    // Xử lý khi bấm nút Đăng nhập
    @FXML
    public void handleLoginAction(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // TODO: Viết logic kiểm tra DB ở đây sau
        System.out.println("Cố gắng đăng nhập với: " + username + " / " + password);
    }

    // Xử lý chuyển sang màn hình Đăng ký
    @FXML
    public void switchToRegister(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Hệ Thống Đấu Giá - Đăng Ký");
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
