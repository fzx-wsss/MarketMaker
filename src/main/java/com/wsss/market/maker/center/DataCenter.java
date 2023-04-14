package com.wsss.market.maker.center;

import com.cmcm.finance.ccc.client.CoinConfigCenterClient;
import com.cmcm.finance.ccc.client.model.SymbolAoWithFeatureAndExtra;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.wsss.market.maker.config.SymbolConfig;
import com.wsss.market.maker.depth.subscribe.bian.BiAnTradeSubscriber;
import com.wsss.market.maker.domain.SymbolInfo;
import com.wsss.market.maker.depth.subscribe.bian.BiAnDepthSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
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

    public synchronized void register(Set<String> symbolNames) {
        log.info("register symbol:{}", symbolNames);
        List<String> childList = new ArrayList<>();
        symbolNames.stream().forEach(s->{
            if(symbolMap.containsKey(s)) {
                return;
            }
            SymbolInfo symbolInfo = BootStrap.getSpringBean(SymbolInfo.class, s);
            symbolMap.put(s, symbolInfo);

            for (String child : symbolInfo.getChildSymbol()) {
                Set set = mappingMap.computeIfAbsent(child, k -> Sets.newConcurrentHashSet());
                if(set.isEmpty()) {
                    childList.add(child);
                }
                set.add(symbolInfo);
            }
        });


        registerDepth(childList);
        registerTrade(childList);
    }

    public synchronized void remove(Set<String> symbolNames) {
        log.info("remove symbol:{}", symbolNames);
        Set<String> childList = new HashSet<>();
        symbolNames.forEach(s -> {
            if (!symbolMap.containsKey(s)) {
                return;
            }
            SymbolInfo symbolInfo = symbolMap.remove(s);

            for (String child : symbolInfo.getChildSymbol()) {
                Set<SymbolInfo> set = mappingMap.get(child);
                set.remove(symbolInfo);
                if (set.isEmpty()) {
                    mappingMap.remove(child);
                    childList.add(child);
                }
            }
        });

        removeDepth(childList);
        removeTrade(childList);
    }

    private void removeDepth(Set<String> symbolInfos) {
        for (BiAnDepthSubscriber subscriber : depthSubscribers) {
            subscriber.remove(symbolInfos);
        }
    }

    private void removeTrade(Set<String> symbolInfos) {
        for (BiAnTradeSubscriber subscriber : tradeSubscribers) {
            subscriber.remove(symbolInfos);
        }
    }

    private void registerDepth(List<String> symbolInfos) {
        int groupSize = symbolConfig.getGroupSize();
        while (!symbolInfos.isEmpty()) {
            for (BiAnDepthSubscriber subscriber : depthSubscribers) {
                int available = groupSize - subscriber.getSubscribedSymbol().size();
                if (available <= 0) {
                    continue;
                }
                List<String> registers = subList(symbolInfos, 0, available);
                symbolInfos = subList(symbolInfos, available, symbolInfos.size());
                subscriber.register(registers);
            }
            break;
        }
        while (!symbolInfos.isEmpty()) {
            BiAnDepthSubscriber depthSubscriber = BootStrap.getSpringBean(BiAnDepthSubscriber.class);
            depthSubscribers.add(depthSubscriber);
            List<String> registers = subList(symbolInfos, 0, groupSize);
            symbolInfos = subList(symbolInfos, groupSize, symbolInfos.size());
            depthSubscriber.register(registers);
        }
    }

    private List<String> subList(List<String> list, int start, int end) {
        if (start >= list.size()) {
            return Collections.EMPTY_LIST;
        }
        if (end > list.size()) {
            return list;
        }
        return list.subList(start, end);
    }



    private void registerTrade(List<String> symbolInfos) {
        int groupSize = symbolConfig.getGroupSize();

        while (!symbolInfos.isEmpty()) {
            for (BiAnTradeSubscriber subscriber : tradeSubscribers) {
                int available = groupSize - subscriber.getSubscribedSymbol().size();
                if (available <= 0) {
                    continue;
                }
                List<String> registers = subList(symbolInfos, 0, available);
                symbolInfos = subList(symbolInfos, available, symbolInfos.size());
                subscriber.register(registers);
            }
            break;
        }
        while (!symbolInfos.isEmpty()) {
            BiAnTradeSubscriber tradeSubscriber = BootStrap.getSpringBean(BiAnTradeSubscriber.class);
            tradeSubscribers.add(tradeSubscriber);
            List<String> registers = subList(symbolInfos, 0, groupSize);
            symbolInfos = subList(symbolInfos, groupSize, symbolInfos.size());
            tradeSubscriber.register(registers);
        }
    }

    public Set<SymbolInfo> getMappingSymbolInfo(String symbol) {
        return mappingMap.getOrDefault(symbol,Collections.EMPTY_SET);
    }

    public boolean isRegistered(String symbol) {
        return symbolMap.containsKey(symbol);
    }

    public Set<String> registeredSymbols() {
        return symbolMap.keySet();
    }
}
