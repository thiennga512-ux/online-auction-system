package com.auction.network;
import com.google.gson.*;
import java.time.LocalDateTime;
import com.auction.enums.ActionType;
import java.time.format.DateTimeFormatter;

public class Request {

   private static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
          new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
      .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
          LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      .create();
      private ActionType action;
      private JsonElement payload;
      public Request(ActionType action,Object payloadObj){
      this.action=action;
      this.payload=GSON.toJsonTree(payloadObj);
}

@SuppressWarnings("unused")
private Request(){}

public ActionType getAction(){
    return action;
}
public <T> T getPayloadAs( Class<T> clazz){
    if(payload==null|| payload.isJsonNull())return null;
    return GSON.fromJson(payload, clazz);
}
public String toJson(){
    return GSON.toJson(this) + "\n"; 
}
public static Request fromJson(String json){
    return GSON.fromJson(json,Request.class);
}

}
