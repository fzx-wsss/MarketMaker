package com.wsss.market.maker.ws;

import java.util.concurrent.TimeUnit;

public interface WSClient {
    void connect();

    void close();

    void send(String msg);

    boolean isAlive();

    void reConnect(int seconds);

    void reConnect();
}
