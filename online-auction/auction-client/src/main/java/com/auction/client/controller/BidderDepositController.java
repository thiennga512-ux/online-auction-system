package com.auction.client.controller;

import com.auction.client.ClientMain;
import com.auction.client.network.SocketClient;
import com.auction.client.util.SessionManager;
import com.auction.common.dto.Dto;
import com.auction.common.network.ActionType;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * ============================================================
 * BidderDepositController — Màn hình nạp tiền cho Bidder
 * ============================================================
 *
 * Cho phép Bidder đã đăng nhập:
 * 1. Xem số dư hiện tại.
 * 2. Chọn nhanh số tiền preset hoặc nhập thủ công.
 * 3. Gửi lệnh SELF_DEPOSIT lên Server.
 * 4. Cập nhật số dư ngay sau khi Server phản hồi thành công.
 * ============================================================
 */
public class BidderDepositController {

    @FXML private Label currentBalanceLabel;
    @FXML private TextField amountField;
    @FXML private Label messageLabel;
    @FXML private Button confirmButton;

    private static final NumberFormat VND_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    // Giữ tham chiếu listener để có thể xoá khi màn hình này bị ẩn
    private Consumer<Response> depositListener;

    @FXML
    public void initialize() {
        // Hiển thị số dư hiện tại từ Session
        updateBalanceDisplay();

        // Đăng ký listener nhận response SELF_DEPOSIT
        depositListener = response -> {
            if (response.getActionType() != ActionType.SELF_DEPOSIT) return;

            Platform.runLater(() -> {
                confirmButton.setDisable(false);
                confirmButton.setText("✅  Xác nhận nạp tiền");

                if (response.isSuccess()) {
                    // Lấy số dư mới từ server
                    Dto.DepositResultResponse result = response.getDataAs(Dto.DepositResultResponse.class);
                    double newBalance = (result != null) ? result.newBalance() : 0;

                    // Cập nhật SessionManager → header tự động update
                    SessionManager.getInstance().updateBalance(newBalance);

                    // Cập nhật label trên màn hình này
                    currentBalanceLabel.setText(formatVND(newBalance));
                    currentBalanceLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #34d399;");

                    // Hiển thị thông báo thành công
                    showSuccess(response.getMessage());
                    amountField.clear();
                } else {
                    showError(response.getMessage());
                }
            });
        };

        SocketClient.getInstance().addListener(depositListener);
    }

    // --- Nút chọn nhanh ---
    @FXML private void selectAmount100k()  { amountField.setText("100000"); }
    @FXML private void selectAmount500k()  { amountField.setText("500000"); }
    @FXML private void selectAmount1M()    { amountField.setText("1000000"); }
    @FXML private void selectAmount5M()    { amountField.setText("5000000"); }
    @FXML private void selectAmount10M()   { amountField.setText("10000000"); }
    @FXML private void selectAmount50M()   { amountField.setText("50000000"); }

    @FXML
    private void handleDeposit() {
        hideMessage();
        String raw = amountField.getText().trim().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) {
            showError("Vui lòng nhập hoặc chọn số tiền cần nạp.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ.");
            return;
        }

        if (amount < 10_000) {
            showError("Số tiền tối thiểu là 10,000 VND.");
            return;
        }

        // Disable nút tránh double-click
        confirmButton.setDisable(true);
        confirmButton.setText("⏳  Đang xử lý...");

        Dto.SelfDepositRequest payload = new Dto.SelfDepositRequest(amount);
        Request request = new Request(ActionType.SELF_DEPOSIT, payload);
        SocketClient.getInstance().sendRequest(request);
    }

    @FXML
    private void handleGoHome() {
        cleanup();
        ClientMain.getMainController().switchContent("home.fxml");
    }

    /** Gọi khi màn hình bị ẩn để tránh memory leak */
    private void cleanup() {
        if (depositListener != null) {
            SocketClient.getInstance().removeListener(depositListener);
            depositListener = null;
        }
    }

    private void updateBalanceDisplay() {
        Dto.UserProfileResponse user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            currentBalanceLabel.setText(formatVND(user.balance()));
        }
    }

    private void showError(String msg) {
        messageLabel.setText("❌  " + msg);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ef4444;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText("✅  " + msg);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34d399;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private String formatVND(double amount) {
        return String.format("%,.0f VND", amount);
    }
}
