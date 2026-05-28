package com.auction.server.service;

import java.util.List;
import java.util.Optional;

import com.auction.enums.UserRole;
import com.auction.exception.BusinessException;
import com.auction.exception.DatabaseException;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;


public class UserService {

  private final UserDAO userDAO;

  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public void setUserActive(String adminId, String targetId, boolean active) {
    if (adminId.equals(targetId)) {
      throw BusinessException.cannotLockOwnAccount();
    }
    try {
      userDAO.setActive(targetId, active);
      String action = active ? "Mở khoá" : "Khoá";
      System.out.println("[UserService] " + action + " user: " + targetId + " bởi Admin: " + adminId);
    } catch (Exception e) {
      throw DatabaseException.queryFailed("thay đổi trạng thái user", e);
    }
  }

 
  public Optional<User> findById(String id) {
    try {
      return userDAO.findById(id);
    } catch (Exception e) {
      throw DatabaseException.queryFailed("tìm user", e);
    }
  }

  public List<User> getUsersByRole(UserRole role) {
    try {
      return userDAO.findAllByRole(role);
    } catch (Exception e) {
      throw DatabaseException.queryFailed("truy vấn user theo role", e);
    }
  }
  public List<User> getAllUsers() {
    try {
      return userDAO.findAll();
    } catch (Exception e) {
      throw DatabaseException.queryFailed("lấy toàn bộ user", e);
    }
  }
  public void depositForBidder(String bidderId, double amount) {
    try {
      Optional<User> opt = userDAO.findById(bidderId);
      if (opt.isEmpty() || !(opt.get() instanceof Bidder bidder)) {
        throw DatabaseException.notFound("Bidder", bidderId);
      }
      bidder.setBalance(bidder.getBalance() + amount);
      userDAO.updateBidderDetails(bidder);
      System.out.printf("[UserService] Nạp %.0f VND cho Bidder: %s%n",
          amount, bidder.getFullName());
    } catch (Exception e) {
      throw DatabaseException.queryFailed("nạp tiền", e);
    }
  }
}
