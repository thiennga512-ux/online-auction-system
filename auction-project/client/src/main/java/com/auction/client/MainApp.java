package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Đường dẫn chuẩn dựa trên cấu trúc resources/fxml/ của bạn
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/fxml/login.fxml"));

        // Tạo scene với kích thước mặc định (bạn có thể chỉnh lại cho vừa giao diện)
        Scene scene = new Scene(fxmlLoader.load());

        // Đổi tên tiêu đề cửa sổ theo yêu cầu của bạn
        stage.setTitle("Hệ Thống Đấu Giá - Đăng Nhập");

        stage.setScene(scene);

        // Bật chế độ Full Screen
        stage.setFullScreen(true);

        // Tùy chỉnh dòng chữ thông báo khi vào Full Screen (mặc định là nhấn ESC để thoát)
        stage.setFullScreenExitHint("Nhấn ESC để thoát chế độ toàn màn hình");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}