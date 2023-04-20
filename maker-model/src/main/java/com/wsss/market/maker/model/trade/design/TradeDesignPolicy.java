package com.wsss.market.maker.model.trade.design;

import com.wsss.market.maker.model.domain.Trade;

public interface TradeDesignPolicy {

    /**
     * 正常情况下，应该如何下撤单
     * @return
     */
    Trade designTrade(Trade trade);

    TradeDesignType getDesignType();
}
