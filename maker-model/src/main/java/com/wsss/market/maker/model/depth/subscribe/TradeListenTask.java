package com.wsss.market.maker.model.depth.subscribe;

public interface TradeListenTask {
    String getSymbol();

    void logTrade();
}
