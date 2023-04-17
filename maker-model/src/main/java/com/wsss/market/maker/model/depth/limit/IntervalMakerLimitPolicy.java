package com.wsss.market.maker.model.depth.limit;

public class IntervalMakerLimitPolicy implements MakerLimitPolicy {
    private int max;
    private int current;

    public IntervalMakerLimitPolicy(int max) {
        this.max = max;
    }

    @Override
    public boolean isOn() {
        current = ++current % max;
        return current == 1;
    }

    @Override
    public LimitType getLimitType() {
        return LimitType.INTERVAL;
    }

}
