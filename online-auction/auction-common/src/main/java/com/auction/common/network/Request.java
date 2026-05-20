package com.auction.common.network;

import com.google.gson.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================
 * Class Request — Biểu diễn một Gói tin gửi từ Client -> Server
 * ============================================================
 *
 * Gói tin JSON sẽ có cấu trúc:
 * {
 *   "action": "LOGIN",
 *   "payload": {
 *      "email": "abc@gmail.com",
 *      "password": "123"
 *   }
 * }
 *
 * `payload` được lưu dưới dạng JsonElement (để trì hoãn việc parse),
 * giúp Controller ở Server ép kiểu tuỳ ý (thành LoginForm, BidForm...).
 * ============================================================
 */
public class Request {
  private static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
          new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
      .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
          LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      .create();

  private ActionType action;
  private JsonElement payload;

  public Request(ActionType action, Object payloadObj) {
    this.action = action;
    this.payload = GSON.toJsonTree(payloadObj);
  }

  // Constructor cho GSON deserialize
  @SuppressWarnings("unused")
  private Request() {}

  public ActionType getAction() {
    return action;
  }

  /**
   * Lấy payload và tự động ép kiểu về class mong muốn.
   * Cực kỳ tiện lợi:
   * LoginForm form = request.getPayloadAs(LoginForm.class);
   */
  public <T> T getPayloadAs(Class<T> clazz) {
    if (payload == null || payload.isJsonNull()) return null;
    return GSON.fromJson(payload, clazz);
  }
  public String toJson() {
    return GSON.toJson(this) + "\n"; // Dấu \n để BufferedReader.readLine() hoạt động
  }
  public static Request fromJson(String json) {
    return GSON.fromJson(json, Request.class);
  }
}
