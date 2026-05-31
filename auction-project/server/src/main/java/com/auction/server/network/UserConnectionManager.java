package com.auction.server.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.network.Response;

/**
 * Quản lý các kết nối WebSocket/Socket của user đang online.
 */
public class UserConnectionManager {
    private static final UserConnectionManager INSTANCE = new UserConnectionManager();

    // Map lưu trữ UserId -> ClientHandler
    private final Map<String, ClientHandler> activeHandlers = new ConcurrentHashMap<>();

    private UserConnectionManager() {
    }

    public static UserConnectionManager getInstance() {
        return INSTANCE;
    }

    public void registerHandler(String userId, ClientHandler handler) {
        activeHandlers.put(userId, handler);
    }

    public void unregisterUser(String userId) {
        activeHandlers.remove(userId);
    }

    public ClientHandler getHandler(String userId) {
        return activeHandlers.get(userId);
    }

    public void broadcastToAdmins(Response response) {
        activeHandlers.forEach((userId, handler) -> {
            handler.sendResponse(response);
        });
    }
}