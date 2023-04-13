package com.wsss.market.maker.ws;

public interface WSListener {
    void receive(String msg);

    void receive(byte[] msg);

    void success();

    void inactive();
}
