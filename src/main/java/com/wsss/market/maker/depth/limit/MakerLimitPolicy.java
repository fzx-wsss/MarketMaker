package com.wsss.market.maker.depth.limit;

public interface MakerLimitPolicy {
    boolean isOn();

    LimitType getLimitType();
}
