package com.wsss.market.maker.model.depth.subscribe;

public interface Subscriber {
    void connect();

    boolean register(String symbol);
}
