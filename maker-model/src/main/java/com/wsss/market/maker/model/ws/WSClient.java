package com.wsss.market.maker.model.ws;

public interface WSClient {
    void connect();

    void close();

    void send(String msg);

    boolean isAlive();

    void reConnect(int seconds);

    void reConnect();
}
