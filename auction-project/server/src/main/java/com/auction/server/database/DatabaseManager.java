package com.auction.server.database;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {
  private static final String DB_HOST = System.getenv().getOrDefault("AUCTION_DB_HOST", "localhost");
  private static final String DB_PORT = System.getenv().getOrDefault("AUCTION_DB_PORT", "3306");
  private static final String DB_NAME = System.getenv().getOrDefault("AUCTION_DB_NAME", "auction_2");
  private static final String DB_USER = System.getenv().getOrDefault("AUCTION_DB_USER", "root");
  private static final String DB_PASSWORD = System.getenv().getOrDefault("AUCTION_DB_PASSWORD", "210607");
  private static final String JDBC_PARAMS = "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
  private static final String ROOT_DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/?" + JDBC_PARAMS;
  private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?" + JDBC_PARAMS;

  private final Set<Connection> allConnections = ConcurrentHashMap.newKeySet();
  private final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

  private static class Holder {
    private static final DatabaseManager INSTANCE = new DatabaseManager();
  }

  private DatabaseManager() {
    initializeDatabase();
  }

  public static DatabaseManager getInstance() {
    return Holder.INSTANCE;
  }

  private void initializeDatabase() {
    try {
      createDatabaseIfMissing();
      try (Connection bootstrapConnection = createTrackedConnection()) {
        try (Statement stmt = bootstrapConnection.createStatement()) {
          stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
        DatabaseInitializer.initialize(bootstrapConnection);
      }
      System.out.println("[DB] Kết nối MySQL thành công: " + DB_URL);
    } catch (SQLException e) {
      System.err.println("[DB] LỖI kết nối MySQL: " + e.getMessage());
      throw new RuntimeException("Không thể khởi động database", e);
    }
  }

  private void createDatabaseIfMissing() throws SQLException {
    try (Connection rootConnection = DriverManager.getConnection(ROOT_DB_URL, DB_USER, DB_PASSWORD);
        Statement stmt = rootConnection.createStatement()) {
      stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    }
  }

  private Connection createTrackedConnection() throws SQLException {
    Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    allConnections.add(conn);
    return conn;
  }

  private Connection getOrCreateThreadConnection() throws SQLException {
    Connection conn = threadConnection.get();
    if (conn == null || conn.isClosed() || !conn.isValid(2)) {
      conn = createTrackedConnection();
      threadConnection.set(conn);
    }
    return conn;
  }

  public Connection getConnection() {
    try {
      return getOrCreateThreadConnection();
    } catch (SQLException e) {
      throw new RuntimeException("[DB] Không thể lấy connection: " + e.getMessage(), e);
    }
  }

  public void close() {
    for (Connection conn : allConnections) {
      try {
        if (conn != null && !conn.isClosed()) {
          conn.close();
        }
      } catch (SQLException e) {
        System.err.println("[DB] Lỗi khi đóng một connection: " + e.getMessage());
      }
    }
    allConnections.clear();
    threadConnection.remove();
    System.out.println("[DB] Đã đóng toàn bộ connection MySQL.");
  }

  public void clearAllDataForTesting() throws SQLException {
    try (Connection connection = createTrackedConnection();
        Statement stmt = connection.createStatement()) {
      stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
      stmt.execute("DELETE FROM bids");
      stmt.execute("DELETE FROM auction_sessions");
      stmt.execute("DELETE FROM electronics_details");
      stmt.execute("DELETE FROM art_details");
      stmt.execute("DELETE FROM vehicle_details");
      stmt.execute("DELETE FROM items");
      stmt.execute("DELETE FROM admin_details");
      stmt.execute("DELETE FROM seller_details");
      stmt.execute("DELETE FROM bidder_details");
      stmt.execute("DELETE FROM users");
      stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
      System.out.println("[DB] Đã xoá toàn bộ data test.");
    }
  }
}
