package com.wsss.market.maker.service.subscribe;

public interface TradeListenTask {
    String getSymbol();

    void logTrade();
}
