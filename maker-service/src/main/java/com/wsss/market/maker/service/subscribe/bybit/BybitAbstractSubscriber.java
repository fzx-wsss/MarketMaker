package com.wsss.market.maker.service.subscribe.bybit;

import com.wsss.market.maker.service.subscribe.AbstractSubscriber;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BybitAbstractSubscriber extends AbstractSubscriber {


    @Override
    protected String getSteamUrl() {
        return sourceConfig.getBybitSteamUrl();
    }

}
