package com.wsss.market.maker.model.depth.limit;

public interface MakerLimitPolicy {
    boolean isOn();

    LimitType getLimitType();
}
