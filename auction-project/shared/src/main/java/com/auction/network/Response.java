package com.auction.network;
import com.google.gson.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.auction.enums.ActionType;
public class Response {

    private static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
          new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
      .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
          LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      .create();
private boolean success;
private String message;
private JsonElement data;
private ActionType actionType;
public static Response success(ActionType action,String message,Object dataObj){
    Response r = new Response();
    r.success=true;
    r.message=message;
    r.actionType=action;
    if(dataObj!=null){
        r.data=GSON.toJsonTree(dataObj);
    }
    return r;
}
public static Response success(ActionType ation,Object dataObj){
    return success(ation,"",dataObj);
}
public static Response error(ActionType action ,String message){
    Response r= new Response();
    r.success=false;
    r.message=message;
    r.actionType=action;
    return r;
}
private Response(){}
public boolean isSuccess(){ return success;}
public String getMessage(){return message;}
public ActionType getActionType(){return actionType;}
public <T> T getDataAs(Class<T> clazz){
    if(data==null|| data.isJsonNull()) return null;
    return GSON.fromJson(data,clazz);
}
public <T> java.util.List<T> getDataAsList(Class<T> clazz){
    if(data==null || data.isJsonNull() || !data.isJsonArray()) return java.util.Collections.emptyList();
    java.util.List<T> list = new java.util.ArrayList<>();
    data.getAsJsonArray().forEach(element ->list.add(GSON.fromJson(element,clazz)));
    return list;
}
public String toJson(){
    return GSON.toJson(this)+"\n";

}
public static Response fromJson(String json){
    return GSON.fromJson(json,Response.class);
}
}
