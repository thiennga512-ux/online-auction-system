package com.Controller;

import java.util.function.Consumer;

import com.ClientMain;
import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.network.Request;
import com.auction.network.Response;
import com.network.SocketClient;
import com.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class BidderDepositController implements LifeCycleAwareController {

  @FXML
  private Label currentBalanceLabel;
  @FXML
  private TextField amountField;
  @FXML
  private Label messageLabel;
  @FXML
  private Button confirmButton;

  private final Consumer<Response> listener = this::handleResponse;

  @FXML
  public void initialize() {
    SocketClient.getInstance().removeListener(listener);
    SocketClient.getInstance().addListener(listener);

    updateBalanceDisplay();
  }

  private void updateBalanceDisplay() {
    Dto.UserProfileResponse user = SessionManager.getInstance().getCurrentUser();
    if (user != null) {
      currentBalanceLabel.setText(String.format("%,.0f VND", user.balance()));
    } else {
      currentBalanceLabel.setText("0 VND");
    }
  }

  @FXML
  private void selectAmount100k() {
    selectAmount(100_000);
  }

  @FXML
  private void selectAmount500k() {
    selectAmount(500_000);
  }

  @FXML
  private void selectAmount1M() {
    selectAmount(1_000_000);
  }

  @FXML
  private void selectAmount5M() {
    selectAmount(5_000_000);
  }

  @FXML
  private void selectAmount10M() {
    selectAmount(10_000_000);
  }

  @FXML
  private void selectAmount50M() {
    selectAmount(50_000_000);
  }

  private void selectAmount(double amount) {
    amountField.setText(String.format("%.0f", amount));
    clearMessage();
  }

  @FXML
  private void handleDeposit() {
    clearMessage();
    String raw = amountField.getText() == null ? "" : amountField.getText().trim();
    if (raw.isEmpty()) {
      showError("Vui lòng chọn hoặc nhập số tiền cần nạp.");
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(raw.replace(",", ""));
    } catch (NumberFormatException e) {
      showError("Số tiền không hợp lệ. Vui lòng nhập số.");
      return;
    }

    if (amount <= 0) {
      showError("Số tiền nạp phải lớn hơn 0 VND.");
      return;
    }

    Dto.UserProfileResponse me = SessionManager.getInstance().getCurrentUser();
    if (me == null) {
      showError("Lỗi hệ thống: Bạn chưa đăng nhập.");
      return;
    }

    confirmButton.setDisable(true);
    messageLabel.setText("Đang gửi yêu cầu nạp tiền...");
    messageLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
    messageLabel.setVisible(true);
    messageLabel.setManaged(true);

    Dto.DepositRequest payload = new Dto.DepositRequest(me.id(), amount);
    SocketClient.getInstance().sendRequest(new Request(ActionType.SELF_DEPOSIT, payload));
  }

  @FXML
  private void handleGoHome() {
    ClientMain.getMainController().switchContent("home.fxml");
  }

  private void handleResponse(Response response) {
    // Luôn bọc trong Platform.runLater để an toàn cho giao diện JavaFX
    Platform.runLater(() -> {
      // Trường hợp 1: Phản hồi từ lệnh nạp của chính mình (để hiện thông báo đợi)
      if (response.getActionType() == ActionType.SELF_DEPOSIT) {
        confirmButton.setDisable(false);
        if (response.isSuccess()) {
          showSuccess(response.getMessage());
          amountField.clear();
        } else {
          showError(response.getMessage());
        }
      }

      // Trường hợp 2: QUAN TRỌNG - Server báo tiền đã về (ActionType.DEPOSIT_BALANCE)
      else if (response.getActionType() == ActionType.DEPOSIT_BALANCE && response.isSuccess()) {
        Dto.DepositResultResponse result = response.getDataAs(Dto.DepositResultResponse.class);
        if (result != null) {
          // Cập nhật ví tiền trong bộ nhớ (Session) và tự động cập nhật header
          SessionManager.getInstance().updateBalance(result.newBalance());

          // RA LỆNH CHO LABEL CẬP NHẬT CON SỐ MỚI trên trang nạp tiền
          updateBalanceDisplay();

          showSuccess("Yêu cầu nạp tiền đã được phê duyệt thành công!");
          System.out.println("✅ UI updated new balance: " + result.newBalance());
        }
      }
    });
  }

  private void clearMessage() {
    messageLabel.setText("");
    messageLabel.setVisible(false);
    messageLabel.setManaged(false);
  }

  private void showError(String msg) {
    messageLabel.setText(msg);
    messageLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
    messageLabel.setVisible(true);
    messageLabel.setManaged(true);
  }

  private void showSuccess(String msg) {
    messageLabel.setText(msg);
    messageLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 13px;");
    messageLabel.setVisible(true);
    messageLabel.setManaged(true);
  }

  @Override
  public void onBeforeHide() {
    SocketClient.getInstance().removeListener(listener);
  }
}
