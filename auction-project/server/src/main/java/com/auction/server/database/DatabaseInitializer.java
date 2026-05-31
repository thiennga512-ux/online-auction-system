package com.auction.server.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseInitializer {

  public static void initialize(Connection connection) {
    try {
      System.out.println("[DB] Khởi tạo schema từ file SQL...");
      executeSqlScript(connection, "schema.sql", false);
      System.out.println("[DB] Khởi tạo indexes từ file SQL...");
      executeSqlScript(connection, "indexes.sql", true);
      System.out.println("[DB] Khởi tạo database thành công.");
    } catch (Exception e) {
      System.err.println("[DB] LỖI khi khởi tạo database: " + e.getMessage());
      throw new RuntimeException("Không thể khởi động/tạo schema database", e);
    }
  }

  private static void executeSqlScript(Connection connection, String resourceName, boolean ignoreErrors) throws Exception {
    try (InputStream is = DatabaseInitializer.class.getClassLoader().getResourceAsStream(resourceName)) {
      if (is == null) {
        throw new IllegalArgumentException("Không tìm thấy file SQL: " + resourceName);
      }
      
      String script;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        script = reader.lines().collect(Collectors.joining("\n"));
      }

      // Tách các câu lệnh SQL bằng dấu chấm phẩy
      String[] statements = script.split(";");
      try (Statement stmt = connection.createStatement()) {
        for (String sql : statements) {
          String trimmedSql = sql.trim();
          if (trimmedSql.isEmpty() || trimmedSql.startsWith("--")) {
            continue;
          }
          try {
            stmt.execute(trimmedSql);
          } catch (SQLException e) {
            if (ignoreErrors) {
              // Bỏ qua lỗi ví dụ như index đã tồn tại
              System.out.println("[DB] Bỏ qua lỗi thực thi SQL (" + resourceName + "): " + e.getMessage());
            } else {
              throw e;
            }
          }
        }
      }
    }
  }
}
