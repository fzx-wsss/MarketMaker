package com.wsss.market.maker.model.depth.subscribe;

public interface DepthListenTask {
    String getSymbol();

    void transferOrderBook();
}
