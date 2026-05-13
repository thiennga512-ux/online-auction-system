package com.network;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import com.auction.network.Request;
import com.auction.network.Response;
/* class client - quan ly ket noi mang cho lcient
nhiem vu : 
 1. Duy trì kết nối TCP Socket đến Server.
 * 2. Lắng nghe Response từ Server liên tục trên một Background Thread
 *    để không làm "đơ" giao diện (UI Thread).
 * 3. Serialize/Deserialize gói tin JSON sử dụng Gson.
 *
 * Áp dụng Callback Pattern:
 * Khi nhận được Response, nó sẽ gọi `onResponseReceived.accept(response)`
 * và dùng `Platform.runLater()` để cập nhật lên UI một cách an toàn.
 */
public class SocketClient {

  private static final String HOST = "127.0.0.1";
  private static final int PORT = 8080;

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  // Background thread để lắng nghe dữ liệu từ Server
  private final ExecutorService executorService;
  
  // Danh sách các Callback để báo cho UI biết khi có Response
  private final List<Consumer<Response>> listeners = new CopyOnWriteArrayList<>();

  // Singleton instance
  private static SocketClient instance;
  private boolean connected = false;

  private SocketClient() {
    executorService = Executors.newSingleThreadExecutor();
  }

  public static synchronized SocketClient getInstance() {
    if (instance == null) {
      instance = new SocketClient();
    }
    return instance;
  }

  public boolean isConnected() {
    return connected && socket != null && !socket.isClosed();
  }

  /**
   * Đăng ký hàm callback.
   */
  public void addListener(Consumer<Response> listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * Huỷ đăng ký callback.
   */
  public void removeListener(Consumer<Response> listener) {
    listeners.remove(listener);
  }

  /**
   * Deprecated: Dùng addListener thay thế để tránh ghi đè.
   */
  @Deprecated
  public void setOnResponseReceived(Consumer<Response> callback) {
    listeners.clear();
    addListener(callback);
  }

  /**
   * Kết nối đến Server.
   */
  public void connect() throws IOException {
    if (isConnected()) {
      return; // Đã kết nối
    }
    
    try {
      socket = new Socket(HOST, PORT);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      
      connected = true;
      System.out.println("[Client] Đã kết nối đến Server " + HOST + ":" + PORT);

      // Bắt đầu luồng lắng nghe
      startListening();
    } catch (IOException e) {
      connected = false;
      throw e;
    }
  }

  /**
   * Gửi một Request lên Server.
   */
  public void sendRequest(Request request) {
    if (!isConnected()) {
        System.err.println("[Client] Chưa kết nối đến Server!");
        
        // Không dùng Platform.runLater nữa
        Response error = Response.error(request.getAction(), "Mất kết nối tới Server.");
        
        // Chạy trực tiếp các listener
        for (Consumer<Response> listener : listeners) {
            listener.accept(error);
        }
        return;
    }
    
    // Phần gửi dữ liệu giữ nguyên
    String json = request.toJson(); 
    out.println(json); // Nên dùng println để có ký tự xuống dòng
    out.flush();
    System.out.println("[Client] -> Sent: " + json.trim());
}

  /**
   * Vòng lặp liên tục chờ đọc dữ liệu từ Server.
   */
  private void startListening() {
    executorService.submit(() -> {
      try {
        String line;
        while ((line = in.readLine()) != null) {
          System.out.println("[Client] <- Received: " + line);
          try {
            Response response = Response.fromJson(line);
            
            // Chuyển việc cập nhật UI sang JavaFX Application Thread
            
              for (Consumer<Response> listener : listeners) {
                try {
                  listener.accept(response);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            ;
            
          } catch (Exception e) {
            System.err.println("[Client] Lỗi parse JSON từ Server: " + line + " | " + e.getMessage());
          }
        }
      } catch (IOException e) {
        System.err.println("[Client] Đã ngắt kết nối khỏi Server.");
      }
    });
  }

  /**
   * Đóng kết nối khi tắt App.
   */
  public void disconnect() {
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
      executorService.shutdownNow();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
