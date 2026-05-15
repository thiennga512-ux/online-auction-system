package com.auction.server.database;

import java.beans.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {
  private static final String DB_HOST = System.getenv().getOrDefault("AUCTION_DB_HOST", "localhost");
  private static final String DB_PORT = System.getenv().getOrDefault("AUCTION_DB_PORT", "3306");
  private static final String DB_NAME = System.getenv().getOrDefault("AUCTION_DB_NAME", "auction_db");
  private static final String DB_USER = System.getenv().getOrDefault("AUCTION_DB_USER", "root");
  private static final String DB_PASSWORD = System.getenv().getOrDefault("AUCTION_DB_PASSWORD", "Phu1234@@");
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
        createTables(bootstrapConnection);
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

  private void createTables(Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("""
          CREATE TABLE IF NOT EXISTS users (
              id            VARCHAR(64) PRIMARY KEY,
              full_name     VARCHAR(255) NOT NULL,
              username      VARCHAR(255) UNIQUE,
              email         VARCHAR(255) NOT NULL UNIQUE,
              password_hash VARCHAR(255) NOT NULL,
              phone_number  VARCHAR(32),
              gender        VARCHAR(32),
              date_of_birth VARCHAR(16),
              created_at    VARCHAR(32) NOT NULL,
              active        TINYINT NOT NULL DEFAULT 1,
              role          VARCHAR(16) NOT NULL
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS admin_details (
              user_id     VARCHAR(64) PRIMARY KEY,
              admin_notes TEXT,
              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS seller_details (
              user_id      VARCHAR(64) PRIMARY KEY,
              shop_name    VARCHAR(255) NOT NULL,
              citizen_id   VARCHAR(32) NOT NULL UNIQUE,
              rating       DOUBLE NOT NULL DEFAULT 0.0,
              rating_count INT NOT NULL DEFAULT 0,
              balance      DOUBLE NOT NULL DEFAULT 0.0,
              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS bidder_details (
              user_id           VARCHAR(64) PRIMARY KEY,
              deposit_balance   DOUBLE NOT NULL DEFAULT 0.0,
              frozen_balance    DOUBLE NOT NULL DEFAULT 0.0,
              shipping_address  TEXT,
              total_bids_placed INT NOT NULL DEFAULT 0,
              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS items (
              id            VARCHAR(64) PRIMARY KEY,
              name          VARCHAR(255) NOT NULL,
              description   TEXT,
              base_price    DOUBLE NOT NULL,
              min_increment DOUBLE NOT NULL,
              seller_id     VARCHAR(64) NOT NULL,
              category      VARCHAR(32) NOT NULL,
              image_url     TEXT,
              available     TINYINT NOT NULL DEFAULT 1,
              listed_at     VARCHAR(32) NOT NULL,
              FOREIGN KEY (seller_id) REFERENCES users(id)
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS electronics_details (
              item_id          VARCHAR(64) PRIMARY KEY,
              brand            VARCHAR(255) NOT NULL,
              model            VARCHAR(255) NOT NULL,
              warranty_months  INT NOT NULL DEFAULT 0,
              condition_type   VARCHAR(64) NOT NULL,
              FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS art_details (
              item_id          VARCHAR(64) PRIMARY KEY,
              artist_name      VARCHAR(255) NOT NULL,
              creation_year    INT DEFAULT 0,
              medium           VARCHAR(255) NOT NULL,
              authenticated    TINYINT NOT NULL DEFAULT 0,
              certificate_id   VARCHAR(255),
              dimensions       VARCHAR(255),
              FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS vehicle_details (
              item_id            VARCHAR(64) PRIMARY KEY,
              vehicle_type       VARCHAR(64) NOT NULL,
              make               VARCHAR(255) NOT NULL,
              model              VARCHAR(255) NOT NULL,
              year               INT NOT NULL,
              mileage            DOUBLE NOT NULL DEFAULT -1,
              fuel_type          VARCHAR(64) NOT NULL,
              transmission       VARCHAR(64) NOT NULL,
              color              VARCHAR(64),
              license_plate      VARCHAR(64),
              has_valid_registry TINYINT NOT NULL DEFAULT 0,
              FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS auction_sessions (
              id                    VARCHAR(64) PRIMARY KEY,
              item_id               VARCHAR(64) NOT NULL,
              seller_id             VARCHAR(64) NOT NULL,
              seller_name           VARCHAR(255) NOT NULL,
              current_price         DOUBLE NOT NULL,
              current_winner_id     VARCHAR(64),
              current_winner_name   VARCHAR(255),
              status                VARCHAR(32) NOT NULL,
              start_time            VARCHAR(32) NOT NULL,
              end_time              VARCHAR(32) NOT NULL,
              actual_end_time       VARCHAR(32) NOT NULL,
              created_at            VARCHAR(32) NOT NULL,
              anti_sniping_seconds  INT NOT NULL DEFAULT 30,
              approved_by_admin_id  VARCHAR(64),
              admin_note            TEXT,
              FOREIGN KEY (item_id) REFERENCES items(id),
              FOREIGN KEY (seller_id) REFERENCES users(id)
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS bids (
              id                  VARCHAR(64) PRIMARY KEY,
              auction_session_id  VARCHAR(64) NOT NULL,
              bidder_id           VARCHAR(64),
              bidder_name         VARCHAR(255) NOT NULL,
              amount              DOUBLE NOT NULL,
              max_auto_bid        DOUBLE,
              timestamp           VARCHAR(32) NOT NULL,
              bid_type            VARCHAR(32) NOT NULL,
              FOREIGN KEY (auction_session_id) REFERENCES auction_sessions(id),
              FOREIGN KEY (bidder_id) REFERENCES users(id)
          )
          """);

      // Tạo Index để tăng tốc truy vấn (MySQL không hỗ trợ IF NOT EXISTS cho CREATE
      // INDEX trực tiếp)
      try {
        stmt.execute("CREATE INDEX idx_bids_session ON bids(auction_session_id)");
        stmt.execute("CREATE INDEX idx_sessions_status ON auction_sessions(status)");
        stmt.execute("CREATE INDEX idx_items_seller ON items(seller_id)");
      } catch (SQLException e) {
        // Bỏ qua nếu index đã tồn tại
      }
      System.out.println("[DB] Tạo schema MySQL thành công");
    }
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
