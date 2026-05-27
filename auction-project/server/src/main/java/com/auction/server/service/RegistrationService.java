package com.auction.server.service;

import com.auction.exception.ValidationException;
import com.auction.exception.BusinessException;
import com.auction.exception.DatabaseException;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.enums.UserRole;
import com.auction.factory.UserFactory;
import com.auction.server.dao.UserDAO;
import java.util.Optional;


public class RegistrationService {

  private final UserDAO userDAO;

  public RegistrationService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }


  public Bidder registerUser(String fullName, String username, String email,
      String password, String gender, String dateOfBirth) {
    validateRegistrationInput(fullName, username, email, password);
    return (Bidder) registerInternal(fullName, username, email, password, UserRole.BIDDER, gender, dateOfBirth);
  }
  public Bidder registerBidder(String fullName, String email, String password) {
    return registerUser(fullName, email.split("@")[0], email, password, "N/A", "1990-01-01");
  }

  public Admin createAdmin(String fullName, String username, String email,
      String password, String gender, String dateOfBirth) {
    validateRegistrationInput(fullName, username, email, password);
    return (Admin) registerInternal(fullName, username, email, password, UserRole.ADMIN, gender, dateOfBirth);
  }

  
  public Admin createAdmin(String fullName, String email, String password) {
    return createAdmin(fullName, email.split("@")[0], email, password, "N/A", "1990-01-01");
  }

  public Seller upgradeToSeller(String userId, String shopName, String citizenId) {
    String normalizedShopName = shopName == null ? "" : shopName.trim();
    String normalizedCitizenId = citizenId == null ? "" : citizenId.trim();

    if (normalizedShopName.isBlank())
      throw ValidationException.required("Tên cửa hàng");
    if (normalizedShopName.length() < 3)
      throw ValidationException.tooShort("Tên cửa hàng", 3);
    if (!normalizedCitizenId.matches("\\d{9,12}"))
      throw new ValidationException("citizenId", "Số CCCD chỉ gồm 9-12 chữ số.");

    try {
      if (userDAO.citizenIdExists(normalizedCitizenId)) {
        throw BusinessException.citizenIdAlreadyUsed();
      }

      Optional<User> optUser = userDAO.findById(userId);
      if (optUser.isEmpty()) {
        throw DatabaseException.notFound("User", userId);
      }
      User user = optUser.get();
      if (user.getRole() == UserRole.SELLER)
        throw BusinessException.alreadySeller();
      if (user.getRole() != UserRole.BIDDER)
        throw BusinessException.canOnlyUpgradeFromBidder();

      userDAO.upgradeToSeller(userId, normalizedShopName, normalizedCitizenId);

      System.out.println("[RegistrationService] Đã nâng cấp thành Seller: " + user.getFullName());

    
      return (Seller) userDAO.findById(userId).orElseThrow();

    } catch (Exception e) {
      throw DatabaseException.queryFailed("nâng cấp Seller", e);
    }
  }

  public Seller registerSeller(String fullName, String email, String password, String shopName) {
    Bidder bidder = registerBidder(fullName, email, password);
    String citizenId = String.format("%012d", Math.abs(email.hashCode() % 1000000000000L));
    return upgradeToSeller(bidder.getId(), shopName, citizenId);
  }

  private User registerInternal(String fullName, String username, String email,
      String password, UserRole role, String gender, String dateOfBirth) {
    String passwordHash = PasswordHasher.hash(password);
    User user = UserFactory.create(role, username, passwordHash, email, fullName, gender, dateOfBirth);
    saveUser(user);
    return user;
  }

  private void saveUser(User user) {
    try {
      userDAO.save(user);
      System.out.println("[RegistrationService] Đã tạo user: " + user);
    } catch (Exception e) {
      throw DatabaseException.queryFailed("lưu user", e);
    }
  }

  private void validateRegistrationInput(String fullName, String username,
      String email, String password) {
    if (fullName == null || fullName.isBlank())
      throw ValidationException.required("Họ tên");
    if (username == null || username.isBlank())
      throw ValidationException.required("Tên đăng nhập");
    if (email == null || !email.contains("@") || !email.contains("."))
      throw ValidationException.invalidEmail(email);
    if (password == null || password.length() < 6)
      throw ValidationException.passwordTooShort(6);

    try {
      if (userDAO.emailExists(email.toLowerCase().trim())) {
        throw BusinessException.emailAlreadyRegistered(email);
      }
      if (userDAO.usernameExists(username.toLowerCase().trim())) {
        throw BusinessException.usernameAlreadyTaken(username);
      }
    } catch (Exception e) {
      throw DatabaseException.queryFailed("kiểm tra email", e);
    }
  }
}
