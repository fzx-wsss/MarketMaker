package com.wsss.market.maker.inner.supplier.bybit;

import com.superatomfin.share.tools.utils.JsonUtils;
import com.wsss.market.maker.inner.api.receive.AbstractSubscriber;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BybitAbstractSubscriber extends AbstractSubscriber {


    @Override
    protected String getSteamUrl() {
        return sourceConfig.getBybitSteamUrl();
    }

    @Override
    public synchronized void checkSelf() {
        BybitSubMsg msg = BybitSubMsg.buildPing();
        sendMsg(JsonUtils.encode(msg));
        super.checkSelf();
    }
}
