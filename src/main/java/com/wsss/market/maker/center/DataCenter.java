package com.wsss.market.maker.center;

import com.cmcm.finance.ccc.client.CoinConfigCenterClient;
import com.google.common.collect.Sets;
import com.wsss.market.maker.config.SymbolConfig;
import com.wsss.market.maker.depth.subscribe.bian.BiAnTradeSubscriber;
import com.wsss.market.maker.domain.SymbolInfo;
import com.wsss.market.maker.depth.subscribe.bian.BiAnDepthSubscriber;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataCenter {
    @Resource
    private CoinConfigCenterClient coinConfigCenterClient;
    @Resource
    private SymbolConfig symbolConfig;

    /**
     * 实际币对名称与实际币对的关系
     */
    private Map<String, SymbolInfo> symbolMap = new ConcurrentHashMap<>();
    /**
     * 映射的币对名称与实际币对的关系
     */
    private Map<String, Set<SymbolInfo>> mappingMap = new ConcurrentHashMap<>();

    private List<BiAnDepthSubscriber> depthSubscribers = new LinkedList<>();
    private List<BiAnTradeSubscriber> tradeSubscribers = new LinkedList<>();

    public SymbolInfo getSymbolInfo(String symbol) {
        return symbolMap.get(symbol);
    }

    public void register(String symbol) {
        SymbolInfo symbolInfo = symbolMap.computeIfAbsent(symbol, s -> BootStrap.createSpringBean(SymbolInfo.class, s));
        for(String child : symbolInfo.getChildSymbol()) {
            mappingMap.computeIfAbsent(child,k-> Sets.newConcurrentHashSet()).add(symbolInfo);
        }

        registerDepth(symbolInfo);
        registerTrade(symbolInfo);
    }

    private void registerDepth(SymbolInfo symbolInfo) {
        for (BiAnDepthSubscriber subscriber : depthSubscribers) {
            if (subscriber.getSubscribedSymbol().size() < symbolConfig.getGroupSize() && subscriber.register(symbolInfo.getChildSymbol())) {
                return;
            }
        }
        BiAnDepthSubscriber depthSubscriber = BootStrap.createSpringBean(BiAnDepthSubscriber.class);
        depthSubscribers.add(depthSubscriber);
        depthSubscriber.register(symbolInfo.getChildSymbol());
    }

    private void registerTrade(SymbolInfo symbolInfo) {
        for (BiAnTradeSubscriber subscriber : tradeSubscribers) {
            if (subscriber.getSubscribedSymbol().size() < symbolConfig.getGroupSize() && subscriber.register(symbolInfo.getChildSymbol())) {
                return;
            }
        }
        BiAnTradeSubscriber tradeSubscriber = BootStrap.createSpringBean(BiAnTradeSubscriber.class);
        tradeSubscribers.add(tradeSubscriber);
        tradeSubscriber.register(symbolInfo.getChildSymbol());
    }

    public Set<SymbolInfo> getMappingSymbolInfo(String symbol) {
        return mappingMap.get(symbol);
    }
}
