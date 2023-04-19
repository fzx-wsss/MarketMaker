package com.wsss.market.maker.model.domain;


import com.wsss.market.maker.model.config.TradeConfig;

import java.util.concurrent.ThreadLocalRandom;

public class TradeLimiter {
    private TradeConfig tradeConfig = TradeConfig.getInstance();
    private SlidingTimeWindow window = new SlidingTimeWindow(tradeConfig.getInterval());
    public boolean isLimit() {
        if (ThreadLocalRandom.current().nextDouble() < (double)window.get() / tradeConfig.getMaxLimit()) {
            return true;
        }
        window.getAndIncrement();
        return false;
    }


}
