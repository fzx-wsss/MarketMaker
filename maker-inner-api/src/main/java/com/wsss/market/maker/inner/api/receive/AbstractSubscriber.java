package com.wsss.market.maker.inner.api.receive;

import com.google.common.collect.Sets;
import com.wsss.market.maker.inner.api.data.SymbolQueryService;
import com.wsss.market.maker.inner.api.data.ThreadPool;
import com.wsss.market.maker.model.config.SourceConfig;
import com.wsss.market.maker.model.config.SymbolConfig;
import com.wsss.market.maker.model.ws.WSClient;
import com.wsss.market.maker.model.ws.WSListener;
import com.wsss.market.maker.model.ws.netty.NettyWSClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractSubscriber implements Subscriber {
    private WSClient wsClient;
    private long lastReceiveTime = System.currentTimeMillis();

    protected SymbolConfig symbolConfig = SymbolConfig.getInstance();
    @Resource
    protected SymbolQueryService symbolQueryService;
    @Resource
    protected ThreadPool markerMakerThreadPool;
    @Resource
    protected SourceConfig sourceConfig;

    @Getter
    protected Set<String> subscribedSymbol = new HashSet<>();

    public void init() {
        try {
            wsClient = NettyWSClient.builder().websocketURI(new URI(getSteamUrl())).wsListener(getWSListener()).build();
            wsClient.connect();
            log.info("subscriber url:{}",getSteamUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected WSListener getWSListener() {
        return this;
    }

    public boolean register(Set<String> symbols) {
        try {
            Set<String> add = Sets.difference(symbols, subscribedSymbol);
            if(add.isEmpty()) {
                return true;
            }
            doRegisterMsg(add);
        } finally {
            subscribedSymbol.addAll(symbols);
        }
        return true;
    }

    protected void reRegister(Collection<String> symbols) {
        if(symbols.isEmpty()) {
            return;
        }
        doRegisterMsg(symbols);
    }

    public boolean remove(Set<String> symbols) {
        try {
            Set<String> remove = new HashSet<>(subscribedSymbol);
            remove.retainAll(symbols);
            if(remove.isEmpty()) {
                return true;
            }
            doRemoveMsg(remove);
        } finally {
            subscribedSymbol.removeAll(symbols);
        }
        return true;
    }

    protected void sendMsg(String sendMsg) {
        log.info("sendMsg:{}",sendMsg);
        wsClient.send(sendMsg);
    }


    @Override
    public void receive(byte[] msg) {

    }

    @Override
    public void success() {
        reRegister(subscribedSymbol);
    }

    @Override
    public void inactive() {

    }

    protected void updateLastReceiveTime() {
        lastReceiveTime = System.currentTimeMillis();
    }

    public synchronized void checkSelf() {
        if(System.currentTimeMillis() - lastReceiveTime > TimeUnit.SECONDS.toMillis(symbolConfig.getMaxReceiveTime()) && !subscribedSymbol.isEmpty()) {
            lastReceiveTime = System.currentTimeMillis();
            wsClient.reConnect(3);
        }
    }

    public void close() {
        wsClient.close();
    }

    protected abstract void doRegisterMsg(Collection<String> symbols);
    protected abstract void doRemoveMsg(Collection<String> symbols);

    protected abstract String getSteamUrl();
}
