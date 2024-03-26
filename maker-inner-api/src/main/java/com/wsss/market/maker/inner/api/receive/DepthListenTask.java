package com.wsss.market.maker.inner.api.receive;

public interface DepthListenTask extends ListenTask {
    void transferOrderBook();
}
