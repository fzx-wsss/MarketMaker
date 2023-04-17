package com.wsss.market.maker.depth.subscribe;

public interface DepthListenTask {
    String getSymbol();

    void transferOrderBook();
}
