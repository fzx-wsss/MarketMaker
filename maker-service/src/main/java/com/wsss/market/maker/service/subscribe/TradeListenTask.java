package com.wsss.market.maker.service.subscribe;

import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.Trade;

public interface TradeListenTask {
    SymbolInfo getSymbol();

    Trade logTrade();
}
