package com.wsss.market.maker.model.limit;

public class AlwaysMakerLimitPolicy implements MakerLimitPolicy {
    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public MakerLimitType getLimitType() {
        return MakerLimitType.ALWAYS;
    }
}
