package com.wsss.market.maker.model.depth.limit;

public class AlwaysMakerLimitPolicy implements MakerLimitPolicy {
    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public LimitType getLimitType() {
        return LimitType.ALWAYS;
    }
}
