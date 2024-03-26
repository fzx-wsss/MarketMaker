package com.wsss.market.maker.inner.api.data;

import com.wsss.market.maker.inner.api.receive.TradeListenTask;

public interface ThreadPool {
    void offerDepth(String symbol, String msg);
    void offerTrade(String symbol, TradeListenTask msg);
}
