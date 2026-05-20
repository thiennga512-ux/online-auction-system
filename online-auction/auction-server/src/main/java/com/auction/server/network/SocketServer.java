package com.auction.server.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 * Class SocketServer — Máy chủ TCP lắng nghe kết nối
 * ============================================================
 *
 * 🎓 THREAD POOL LÀ GÌ?
 * Thay vì `new Thread().start()` cho mỗi client (có thể sinh hàng nghìn thread gây sập Ram).
 * Ta dùng `ExecutorService` để tạo ra ThreadPool (hồ bơi luồng).
 * VD: Executors.newFixedThreadPool(100) -> Chỉ giới hạn tối đa 100 User kết nối cùng lúc.
 * ============================================================
 */
public class SocketServer {

  private static final int PORT = 8080; // cong mà server sẽ lắng nghe kết nối TCP
  private static final int CORE_THREADS = 50; //so luong nhan vien tuc truc binh thuong
  private static final int MAX_THREADS = 300;// so luong nhan vien tuc truc toi da khi co su co hoac can tang ca do
  private static final int QUEUE_CAPACITY = 2000;// so luong khach hang toi da doi
  
  // ThreadPool đàn hồi + queue lớn để xử lý đồng thời nhiều bidder/seller
  private final ExecutorService threadPool = new ThreadPoolExecutor(
      CORE_THREADS,
      MAX_THREADS,
      60L,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(QUEUE_CAPACITY),
      new ThreadPoolExecutor.CallerRunsPolicy()
  );
  private final RequestDispatcher dispatcher;

  public SocketServer(RequestDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  public void start() {
    System.out.printf("[SocketServer] ✅ Bắt đầu lắng nghe TCP Socket tại cổng %d...%n", PORT);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      threadPool.shutdown();
      System.out.println("[SocketServer] Thread pool đã được shutdown.");
    }));
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      while (true) {
        // Chặn ở đây để đợi Client kết nối
        Socket clientSocket = serverSocket.accept();
        System.out.println("[SocketServer] 🔗 Có kết nối mới từ: " + clientSocket.getRemoteSocketAddress());

        // Giao việc đọc/ghi cho ThreadPool xử lý -> ServerSocket tiếp tục accept người mới lập tức
        ClientHandler handler = new ClientHandler(clientSocket, dispatcher);
        threadPool.execute(handler);
      }
    } catch (Exception e) {
      System.err.println("[SocketServer] ❌ Lỗi máy chủ: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
