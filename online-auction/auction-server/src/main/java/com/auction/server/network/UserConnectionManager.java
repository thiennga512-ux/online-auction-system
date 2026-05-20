package com.auction.server.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserConnectionManager {
    private static final UserConnectionManager instance = new UserConnectionManager();
    private final Map<String, ClientHandler> activeUsers = new ConcurrentHashMap<>();

    private UserConnectionManager() {}

    public static UserConnectionManager getInstance() {
        return instance;
    }

    public void registerUser(String userId, ClientHandler handler) {
        if (userId != null) {
            activeUsers.put(userId, handler);
        }
    }

    public void unregisterUser(String userId) {
        if (userId != null) {
            activeUsers.remove(userId);
        }
    }

    public ClientHandler getHandler(String userId) {
        return activeUsers.get(userId);
    }

    public java.util.Collection<ClientHandler> getActiveHandlers() {
        return activeUsers.values();
    }
}
