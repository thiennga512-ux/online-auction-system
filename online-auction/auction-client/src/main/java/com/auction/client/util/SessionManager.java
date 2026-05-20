package com.auction.client.util;

import com.auction.common.dto.Dto.UserProfileResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ============================================================
 * SessionManager — Quản lý trạng thái đăng nhập Client
 * ============================================================
 * 
 * Là Singleton giúp mọi màn hình (Controller) có thể biết:
 * 1. Ai đang đăng nhập?
 * 2. Cập nhật giao diện khi login/logout thành công.
 * ============================================================
 */
public class SessionManager {

  private static final SessionManager INSTANCE = new SessionManager();

  private UserProfileResponse currentUser;
  private String selectedAuctionSessionId;
  
  // Danh sách các hàm sẽ được gọi khi trạng thái login thay đổi (Observer Pattern)
  private final List<Consumer<UserProfileResponse>> listeners = new ArrayList<>();

  private SessionManager() {}

  public static SessionManager getInstance() {
    return INSTANCE;
  }

  /**
   * Lấy thông tin user đang đăng nhập. Trả về null nếu chưa đăng nhập (Khách).
   */
  public UserProfileResponse getCurrentUser() {
    return currentUser;
  }

  /**
   * Cập nhật thông tin user sau khi Đăng nhập hoặc Đăng xuất.
   * Gọi tất cả các listener để UI tự động thay đổi.
   */
  public void setCurrentUser(UserProfileResponse user) {
    this.currentUser = user;
    for (Consumer<UserProfileResponse> listener : listeners) {
      listener.accept(this.currentUser);
    }
  }

  /**
   * Cập nhật số dư của user hiện tại (sau khi nạp tiền thành công).
   * Tạo ra bản record mới vì UserProfileResponse là immutable record.
   */
  public void updateBalance(double newBalance) {
    if (currentUser == null) return;
    this.currentUser = new UserProfileResponse(
        currentUser.id(), currentUser.fullName(), currentUser.role(), newBalance);
    for (Consumer<UserProfileResponse> listener : listeners) {
      listener.accept(this.currentUser);
    }
  }

  public boolean isLoggedIn() {
    return currentUser != null;
  }

  public String getSelectedAuctionSessionId() {
    return selectedAuctionSessionId;
  }

  public void setSelectedAuctionSessionId(String selectedAuctionSessionId) {
    this.selectedAuctionSessionId = selectedAuctionSessionId;
  }

  /**
   * Đăng ký listener. MainController sẽ dùng hàm này để biết khi nào cần đổi Header.
   */
  public void addLoginStateListener(Consumer<UserProfileResponse> listener) {
    listeners.add(listener);
    // Kích hoạt ngay lập tức với state hiện tại
    listener.accept(this.currentUser);
  }
}
