package com.auction.network;

import com.google.gson.*;// sau test serializable
import java.time.LocalDateTime;
import com.auction.enums.ActionType;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================
 * Class Request — Biểu diễn một Gói tin gửi từ Client -> Server
 * ============================================================
 */
public class Request {
    /**
     * Bộ máy GSON được cấu hình riêng để xử lý các kiểu dữ liệu phức tạp.
     * Sử dụng GsonBuilder để "dạy" Java cách biến ngày tháng thành văn bản và ngược
     * lại.
     */
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc,
                            context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime
                            .parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();
    private ActionType action;
    private JsonElement payload;

    public Request(ActionType action, Object payloadObj) {
        this.action = action;
        this.payload = GSON.toJsonTree(payloadObj);
    }

    // Constructor rong de Gson do du lieu vao
    @SuppressWarnings("unused")
    private Request() {
    }

    public ActionType getAction() {
        return action;
    }
    // lay payload va tung ep kieu ve class mong muon

    public <T> T getPayloadAs(Class<T> clazz) {
        if (payload == null || payload.isJsonNull())
            return null;
        return GSON.fromJson(payload, clazz);
    }

    public String toJson() {
        return GSON.toJson(this) + "\n"; // Dấu \n để BufferedReader.readLine() hoạt động
    }

    public static Request fromJson(String json) {
        return GSON.fromJson(json, Request.class);
    }

}
