package com.auction.client;

/**
 * ============================================================
 * Lớp Launcher (Mẹo để chạy JavaFX trong IDE)
 * ============================================================
 * 
 * Tại sao cần class này?
 * Kể từ Java 11+, JavaFX không còn nằm trong JDK mặc định.
 * Nếu bạn chạy trực tiếp class ClientMain (kế thừa Application), 
 * IDE có thể ném lỗi "Components are missing".
 * 
 * Cách giải quyết: Tạo một class bình thường không kế thừa Application,
 * và gọi ClientMain.main(args) từ đây. IDE sẽ chạy mượt mà!
 */
public class Launcher {
    public static void main(String[] args) {
        ClientMain.main(args);
    }
}
