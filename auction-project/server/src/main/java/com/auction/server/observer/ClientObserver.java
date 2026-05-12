package com.auction.server.observer;
import com.auction.network.Response;

public interface ClientObserver {
    void onUpdate(Response response);
    // lay id cua user dang ket noi de khong tu gui lai bid cua chinh minh neu can
    String getUserId();
}
