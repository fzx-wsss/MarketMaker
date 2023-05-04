package com.wsss.market.maker.service.subscribe;

import com.wsss.market.maker.model.domain.Trade;

public interface TradeListenTask extends ListenTask {
    Trade logTrade();
}
