package com.wsss.market.maker.model.limit;

public interface MakerLimitPolicy {
    boolean isOn();

    MakerLimitType getLimitType();
}
