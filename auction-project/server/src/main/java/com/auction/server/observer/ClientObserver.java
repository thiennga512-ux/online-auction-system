package com.auction.server.observer;

import com.auction.network.Response;

public interface ClientObserver {
    void onUpdate(Response response);

    String getUserId();
}
