package com.wsss.market.maker.inner.api.receive;

import com.wsss.market.maker.model.ws.WSListener;

import java.util.Set;

public interface Subscriber extends WSListener {

    boolean register(Set<String> symbols);
    boolean remove(Set<String> symbols);

    Set<String> getSubscribedSymbol();

    void init();
    void close();
    void checkSelf();
}
