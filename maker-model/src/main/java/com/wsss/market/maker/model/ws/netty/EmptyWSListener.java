package com.wsss.market.maker.model.ws.netty;


import com.wsss.market.maker.model.ws.WSListener;

public class EmptyWSListener implements WSListener {
    public void receive(String msg) {

    }

    public void receive(byte[] msg) {

    }

    @Override
    public void success() {

    }

    @Override
    public void inactive() {

    }
}
