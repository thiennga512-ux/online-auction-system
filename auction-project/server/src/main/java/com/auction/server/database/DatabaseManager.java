package com.auction.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String DB_HOST = "zephyr.proxy.rlwy.net"; 
    private static final String DB_PORT = "59552";                         
    private static final String DB_NAME = "railway";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "jXrPfSulOmAzFJwdTsjkdZiVkUjntyCS";

    // CẤU HÌNH KẾT NỐI: Tối ưu cho Cloud Database (Railway)
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useSSL=true"
        + "&trustServerCertificate=true"
        + "&serverTimezone=UTC"
        + "&allowPublicKeyRetrieval=true";

    private final HikariDataSource dataSource;

    private static class Holder {
        private static final DatabaseManager INSTANCE = new DatabaseManager();
    }

    private DatabaseManager() {
        try {
            System.out.println("[DB] Khởi tạo HikariCP Connection Pool...");
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USER);
            config.setPassword(DB_PASSWORD);
            
            // Tùy chỉnh Pool (giữ các kết nối hot luôn sẵn sàng phục vụ)
            config.setMaximumPoolSize(15);      // Số kết nối tối đa mở sẵn
            config.setMinimumIdle(5);           // Giữ tối thiểu 5 kết nối nóng, tránh kết nối lại
            config.setConnectionTimeout(10000); // Giảm timeout xuống 10s để fail-fast khi mạng có sự cố
            config.setIdleTimeout(300000);      // 5 phút idle
            config.setMaxLifetime(1800000);     // 30 phút tuổi thọ tối đa mỗi kết nối

            // Tối ưu hóa driver MySQL để giảm thiểu các gói tin truy vấn cấu hình không cần thiết
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.addDataSourceProperty("useCompression", "true"); // Nén dữ liệu truyền tải trên mạng WAN

            dataSource = new HikariDataSource(config);

            // Thử lấy connection lần đầu để test
            try (Connection conn = dataSource.getConnection()) {
                System.out.println("[DB] Kết nối Railway Cloud (qua HikariCP) thành công!");
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể kết nối Database bằng HikariCP!", e);
        }
    }

    public static DatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    public Connection getConnection() {
        try {
            // HikariCP sẽ trả kết nối đã mở sẵn, cực kỳ nhanh
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy Connection từ Pool: " + e.getMessage(), e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DB] Đã đóng HikariCP Connection Pool.");
        }
    }
}