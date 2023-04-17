package com.wsss.market.maker.model.depth.limit;

public class FixedMakerLimitPolicy implements MakerLimitPolicy {
    private long lastTime;
    private long fixedTime;

    public FixedMakerLimitPolicy(long fixedTime) {
        this.fixedTime = fixedTime;
    }

    @Override
    public boolean isOn() {
        return System.currentTimeMillis() - lastTime > fixedTime;
    }

    @Override
    public LimitType getLimitType() {
        return LimitType.FIXED;
    }
}
