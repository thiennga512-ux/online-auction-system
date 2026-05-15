package com.auction.server.service;

import java.util.Optional;

import com.auction.exception.AuthException;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;

public class AuthService {
    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User login(String email, String rawPassword) {
        Optional<User> optUser = userDAO.findByEmail(email);

        if (optUser.isEmpty()) {
            throw AuthException.invalidCredentials();
        }

        User user = optUser.get();

        if (!user.isActive()) {
            throw AuthException.accountLocked();
        }

        String hashedInput = PasswordHasher.hash(rawPassword);
        if (!user.getPasswordHash().equals(hashedInput)) {
            throw AuthException.invalidCredentials();
        }

        return user;
    }
}
