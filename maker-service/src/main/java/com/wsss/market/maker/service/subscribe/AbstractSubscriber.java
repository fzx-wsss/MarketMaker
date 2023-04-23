package com.wsss.market.maker.service.subscribe;

import com.wsss.market.maker.model.config.SymbolConfig;
import com.wsss.market.maker.model.ws.WSClient;
import com.wsss.market.maker.model.ws.WSListener;
import com.wsss.market.maker.model.ws.netty.NettyWSClient;
import com.wsss.market.maker.service.center.DataCenter;
import com.wsss.market.maker.service.thread.pool.MarkerMakerThreadPool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractSubscriber {
    private WSClient wsClient;
    private long lastReceiveTime = System.currentTimeMillis();

    protected SymbolConfig symbolConfig = SymbolConfig.getInstance();
    @Resource
    protected DataCenter dataCenter;
    @Resource
    protected MarkerMakerThreadPool markerMakerThreadPool;

    @Getter
    protected Set<String> subscribedSymbol = new HashSet<>();


    @PostConstruct
    public void init() {
        try {
            wsClient = NettyWSClient.builder().websocketURI(new URI(getSteamUrl())).wsListener(getWSListener()).build();
            wsClient.connect();
            log.info("subscriber url:{}",getSteamUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean register(Collection<String> symbols) {
        if(symbols.isEmpty()) {
            return true;
        }
        try {
            doRegisterMsg(symbols);
        } finally {
            subscribedSymbol.addAll(symbols);
        }
        return true;
    }

    public boolean remove(Set<String> symbols) {
        if(symbols.isEmpty()) {
            return true;
        }
        try {
            doRemoveMsg(symbols);
        } finally {
            subscribedSymbol.removeAll(symbols);
        }
        return true;
    }

    protected void sendMsg(String sendMsg) {
        log.info("bi an sendMsg:{}",sendMsg);
        wsClient.send(sendMsg);
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
    protected abstract WSListener getWSListener();


}
