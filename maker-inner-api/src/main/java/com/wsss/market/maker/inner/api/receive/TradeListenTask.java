package com.wsss.market.maker.inner.api.receive;

import com.wsss.market.maker.model.domain.Trade;

public interface TradeListenTask extends ListenTask {
    Trade logTrade();
}
