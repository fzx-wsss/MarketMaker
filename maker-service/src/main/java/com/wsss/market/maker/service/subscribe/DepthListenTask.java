package com.wsss.market.maker.service.subscribe;

public interface DepthListenTask extends ListenTask {
    void transferOrderBook();
}
