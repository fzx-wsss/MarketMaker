package com.wsss.market.maker.domain;

import com.wsss.market.maker.config.TradeConfig;
import lombok.Getter;
import org.apache.commons.math3.util.FastMath;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

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
