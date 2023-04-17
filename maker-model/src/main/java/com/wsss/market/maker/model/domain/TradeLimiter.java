package com.wsss.market.maker.model.domain;


import com.wsss.market.maker.model.config.TradeConfig;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class TradeLimiter {
    private AtomicInteger count = new AtomicInteger(0);
    private TradeConfig tradeConfig = TradeConfig.getInstance();
    private SlidingTimeWindow window = new SlidingTimeWindow(tradeConfig.getInterval());
    public boolean isLimit() {
        if (ThreadLocalRandom.current().nextDouble() < window.getAndIncrement() / tradeConfig.getMaxLimit()) {
            return true;
        }
        return false;
    }


}
