package com.auction.client;

import com.auction.client.controller.MainController;
import com.auction.client.network.SocketClient;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ============================================================
 * ClientMain — Điểm khởi chạy của Client JavaFX
 * ============================================================
 *
 * Nhiệm vụ:
 * 1. Khởi động vòng đời ứng dụng JavaFX (kế thừa Application).
 * 2. Kết nối tới Server thông qua SocketClient.
 * 3. Quản lý việc chuyển đổi giữa các màn hình (SceneManager đơn giản).
 * ============================================================
 */
public class ClientMain extends Application {

  private static Stage primaryStage;
  private static MainController mainController;

  public static void setMainController(MainController controller) {
    mainController = controller;
  }

  public static MainController getMainController() {
    return mainController;
  }

  @Override
  public void start(Stage stage) throws IOException {
    primaryStage = stage;
    
    // Kết nối đến Server
    try {
      SocketClient.getInstance().connect();
    } catch (IOException e) {
      System.err.println("Không thể kết nối đến Server. Vui lòng bật Server trước.");
      // Tuỳ chọn: Hiện thông báo lỗi lên màn hình hoặc thoát
    }

    // Load Layout chính (SPA)
    try {
      Parent root = FXMLLoader.load(getClass().getResource("/fxml/main_layout.fxml"));
      Scene scene = new Scene(root);
      
      // Load CSS toàn cục
      String css = getClass().getResource("/css/style.css").toExternalForm();
      scene.getStylesheets().add(css);

      primaryStage.setTitle("Online Auction - Nền tảng Đấu giá Hiện đại");
      primaryStage.setScene(scene);
      // 1. Cho phép thay đổi kích thước (Bắt buộc để Maximize)
      primaryStage.setResizable(true);

      // 2. Lệnh phóng to cửa sổ tối đa ngay khi mở
      primaryStage.setMaximized(true);

      primaryStage.show();
    } catch (IOException e) {
      System.err.println("Lỗi khi load main_layout.fxml");
      e.printStackTrace();
    }
  }

  @Override
  public void stop() throws Exception {
    // Đảm bảo đóng kết nối Socket khi tắt ứng dụng
    SocketClient.getInstance().disconnect();
    super.stop();
  }

  /**
   * Cũ: Đổi toàn bộ Scene.
   * Mới: Đã bị loại bỏ để dùng MainController.switchContent()
   */
  @Deprecated
  public static void switchScene(String fxmlFile, String title) {
    // Không dùng nữa, đổi qua: ClientMain.getMainController().switchContent("...");
  }

  public static void main(String[] args) {
    // Lệnh khởi động JavaFX
    launch(args);
  }
}
