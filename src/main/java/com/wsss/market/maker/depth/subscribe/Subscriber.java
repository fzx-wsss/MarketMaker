package com.wsss.market.maker.depth.subscribe;

public interface Subscriber {
    void connect();

    boolean register(String symbol);
}
