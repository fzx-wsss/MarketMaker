package com.wsss.market.maker.service.subscribe;

public interface DepthListenTask {
    String getSymbol();

    void transferOrderBook();
}
