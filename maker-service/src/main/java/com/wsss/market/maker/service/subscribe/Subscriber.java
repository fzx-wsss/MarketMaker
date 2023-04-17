package com.wsss.market.maker.service.subscribe;

public interface Subscriber {
    void connect();

    boolean register(String symbol);
}
