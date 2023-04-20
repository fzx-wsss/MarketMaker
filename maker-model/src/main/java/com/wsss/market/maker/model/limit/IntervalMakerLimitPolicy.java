package com.wsss.market.maker.model.limit;

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
    public MakerLimitType getLimitType() {
        return MakerLimitType.INTERVAL;
    }

}
