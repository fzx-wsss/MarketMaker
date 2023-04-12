package com.wsss.market.maker.depth.subscribe;

public interface TradeListenTask {
    String getSymbol();

    void logTrade();
}
