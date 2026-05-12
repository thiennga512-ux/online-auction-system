package com.auction.server.network;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
public class SocketServer {
    private static final int PORT = 8080;//port server lang nghe ket noi tu client
    private static final int CORE_THREADS=50;//so luong thread co san de xu li ket noi, neu het se tao them den MAX_THREADS neu can, neu van het se cho den khi co thread xong viec
    private static final int MAX_THREADS=300;//so luong thread toi da co the tao de xu li ket noi, neu het se tu cho den khi co thread xong viec
    private static final int QUEUE_CAPACITY= 2000;//so luong ket noi toi da co the cho vao hang doi de xu li, neu het se tu cho den khi co thread xong viec
    //ThreadPool dan hoi + queue lon de xu li dong thoi nhieu bidder/seller
    private final ExecutorService threadPool = new ThreadPoolExecutor(
        CORE_THREADS,
        MAX_THREADS,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(QUEUE_CAPACITY),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
    private final RequestDispatcher dispatcher;
    public SocketServer(RequestDispatcher dispatcher){
        this.dispatcher=dispatcher;
    }
    public void start(){
        System.out.printf("[SocketServer] ✅ Bắt đầu lắng nghe TCP Socket tại cổng %d...%n", PORT);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      threadPool.shutdown();
      System.out.println("[SocketServer] Thread pool đã được shutdown.");
    }));
    try (ServerSocket serverSocket = new ServerSocket(PORT)){
        while(true){
            Socket clientSocket= serverSocket.accept();
            System.out.println("[SocketServer] 🔌 Kết nối mới từ " + clientSocket.getRemoteSocketAddress());
            //Giao việc đọc/ghi cho ThreadPool xử lý -> ServerSocket tiếp tục accept người mới lập tức
            ClientHandler handler= new ClientHandler(clientSocket,dispatcher);
            threadPool.execute(handler);
        }
    }catch(IOException e){
        System.err.println("[SocketServer] Lỗi khi khởi động server: " + e.getMessage());
    }
}
}
