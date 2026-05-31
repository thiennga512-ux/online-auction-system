package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private static final String DB_HOST = "zephyr.proxy.rlwy.net"; 
    private static final String DB_PORT = "59552";                         
    private static final String DB_NAME = "railway";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "jXrPfSulOmAzFJwdTsjkdZiVkUjntyCS";

    // CẤU HÌNH KẾT NỐI: Phải có useSSL=true và trustServerCertificate=true cho môi trường Cloud
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useSSL=true&trustServerCertificate=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private final Set<Connection> allConnections = ConcurrentHashMap.newKeySet();
    private final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

    private static class Holder {
        private static final DatabaseManager INSTANCE = new DatabaseManager();
    }

    private DatabaseManager() {
        try {
            // Kiểm tra kết nối khi khởi tạo
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                System.out.println("[DB] Kết nối Railway Cloud thành công!");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối Database. Hãy kiểm tra Host/Port trong Railway!", e);
        }
    }

    public static DatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    public Connection getConnection() {
        try {
            Connection conn = threadConnection.get();
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                allConnections.add(conn);
                threadConnection.set(conn);
            }
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy Connection: " + e.getMessage(), e);
        }
    }

    public void close() {
        for (Connection conn : allConnections) {
            try {
                if (conn != null && !conn.isClosed()) conn.close();
            } catch (SQLException e) {
                System.err.println("Lỗi đóng connection: " + e.getMessage());
            }
        }
        allConnections.clear();
        threadConnection.remove();
    }
}