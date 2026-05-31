package com.auction.server.network;

import java.io.*;
import java.net.Socket;
import com.auction.dto.Dto;
import com.auction.enums.ActionType;
import com.auction.network.*;
import com.auction.server.observer.*;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable, ClientObserver {

  private final Socket socket;
  private final RequestDispatcher dispatcher;

  private BufferedReader in;
  private BufferedWriter out;
  private final Object writeLock = new Object();

  // ID của user sau khi login thành công (-1 hoặc null nếu chưa login)
  private volatile String loggedInUserId = null;

  public ClientHandler(Socket socket, RequestDispatcher dispatcher) {
    this.socket = socket;
    this.dispatcher = dispatcher;
  }

  @Override
  public void run() {
    System.out.println("[ClientHandler] Client connected: " + socket.getRemoteSocketAddress());
    try {
      // Setup các luồng I/O
      in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

      String jsonLine;
      // Vòng lặp nhận Request từ Client
      while ((jsonLine = in.readLine()) != null) {
        if (jsonLine.trim().isEmpty())
          continue;

        try {
          Request request = Request.fromJson(jsonLine);

          // Xử lý logic Đăng ký Observer ngay tại mức Handler
          if (request.getAction() == ActionType.SUBSCRIBE_AUCTION) {
            handleSubscribe(request);
          } else if (request.getAction() == ActionType.UNSUBSCRIBE_AUCTION) {
            handleUnsubscribe(request);
          } else {
            // Các request khác -> Đẩy cho Dispatcher
            Response response = dispatcher.dispatch(request, this);
            sendResponse(response);
          }

        } catch (Throwable e) {
          System.err.println("[ClientHandler] Lỗi xử lý JSON hoặc Lỗi Hệ Thống: " + e.getMessage());
          e.printStackTrace();

          // Trả về lỗi kèm theo ActionType của request (nếu parse được request)
          ActionType action = null;
          try {
            Request req = Request.fromJson(jsonLine);
            action = req.getAction();
          } catch (Exception ignored) {
          }

          sendResponse(Response.error(action, "Lỗi máy chủ nội bộ: " + e.getMessage()));
        }
      }

    } catch (Exception e) {
      System.out.println("[ClientHandler] Bị ngắt kết nối: " + socket.getRemoteSocketAddress());
    } finally {
      disconnect();
    }
  }

  private void handleSubscribe(Request request) {
    Dto.SubscribeRequest payload = request.getPayloadAs(Dto.SubscribeRequest.class);
    if (payload != null && payload.sessionId() != null) {
      AuctionBroadcaster.getInstance().subscribe(payload.sessionId(), this);
      sendResponse(Response.success(ActionType.SUBSCRIBE_AUCTION,
          "Đăng ký nhận tin thành công cho phiên " + payload.sessionId()));
    }
  }

  private void handleUnsubscribe(Request request) {
    Dto.SubscribeRequest payload = request.getPayloadAs(Dto.SubscribeRequest.class);
    if (payload != null && payload.sessionId() != null) {
      AuctionBroadcaster.getInstance().unsubscribe(payload.sessionId(), this);
    }
  }

  /**
   * Gửi response object về Client dưới dạng chuỗi JSON.
   */
  public void sendResponse(Response response) {
    synchronized (writeLock) {
      try {
        if (out != null && !socket.isClosed()) {
          out.write(response.toJson());
          out.flush();
        }
      } catch (Exception e) {
        System.err.println("[ClientHandler] Lỗi gửi data: " + e.getMessage());
      }
    }
  }

  /** Xử lý dọn dẹp khi ngắt kết nối */
  private void disconnect() {
    try {
      if (loggedInUserId != null) {
          UserConnectionManager.getInstance().unregisterUser(loggedInUserId);
      }
      AuctionBroadcaster.getInstance().unsubscribeAll(this);
      if (socket != null && !socket.isClosed())
        socket.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // --- Observer implementation ---
  @Override
  public void onUpdate(Response response) {
    // Được Broadcaster gọi từ một Thread khác
    sendResponse(response);
  }

  @Override
  public String getUserId() {
    return loggedInUserId != null ? loggedInUserId : "Anonymous";
  }

  // --- Getter/Setter cho trạng thái đăng nhập ---
  public String getLoggedInUserId() {
    return loggedInUserId;
  }

  public void setLoggedInUserId(String loggedInUserId) {
    this.loggedInUserId = loggedInUserId;
  }
}
