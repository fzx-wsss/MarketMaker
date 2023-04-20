package com.wsss.market.maker.model.limit;


import com.wsss.market.maker.model.config.TradeConfig;
import com.wsss.market.maker.model.domain.SlidingTimeWindow;

import java.util.concurrent.ThreadLocalRandom;

public class SlidingTimeMakerLimitPolicy implements MakerLimitPolicy {
    private TradeConfig tradeConfig = TradeConfig.getInstance();
    private SlidingTimeWindow window = new SlidingTimeWindow(tradeConfig.getInterval());
    @Override
    public boolean isOn() {
        if (ThreadLocalRandom.current().nextDouble() < (double)window.get() / tradeConfig.getMaxLimit()) {
            return false;
        }
        window.getAndIncrement();
        return true;
    }

    @Override
    public MakerLimitType getLimitType() {
        return MakerLimitType.SLIDING_TIME;
    }


}
