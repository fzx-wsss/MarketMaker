package com.wsss.market.maker.ws.netty;


import com.wsss.market.maker.ws.WSListener;

public class EmptyWSListener implements WSListener {
    public void receive(String msg) {

    }

    public void receive(byte[] msg) {

    }
}
