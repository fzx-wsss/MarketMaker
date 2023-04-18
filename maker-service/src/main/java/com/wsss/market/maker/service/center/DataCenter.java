package com.wsss.market.maker.service.center;

import com.cmcm.finance.ccc.client.CoinConfigCenterClient;
import com.google.common.collect.Sets;
import com.wsss.market.maker.model.config.SymbolConfig;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.service.subscribe.bian.BiAnDepthSubscriber;
import com.wsss.market.maker.service.subscribe.bian.BiAnTradeSubscriber;
import com.wsss.market.maker.service.task.QueryOwnerOrderTask;
import com.wsss.market.maker.service.thread.pool.MarkerMakerThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Slf4j
@Component
public class DataCenter {
    @Resource
    private CoinConfigCenterClient coinConfigCenterClient;
    @Resource
    private SymbolConfig symbolConfig;
    @Resource
    private MarkerMakerThreadPool makerPool;

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
        List<Future> syncTaskFuture = new ArrayList<>();
        symbolNames.stream().forEach(s->{
            if(symbolMap.containsKey(s)) {
                return;
            }
            SymbolInfo symbolInfo = ApplicationUtils.getSpringBean(SymbolInfo.class, s);
            symbolMap.put(s, symbolInfo);
            QueryOwnerOrderTask orderTask = QueryOwnerOrderTask.builder().symbol(symbolInfo).build();
            syncTaskFuture.add(makerPool.execAsyncTask(orderTask));

            for (String child : symbolInfo.getChildSymbol()) {
                Set set = mappingMap.computeIfAbsent(child, k -> Sets.newConcurrentHashSet());
                if(set.isEmpty()) {
                    childList.add(child);
                }
                set.add(symbolInfo);
            }
        });
        syncTaskFuture.forEach(f-> {
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
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
            BiAnDepthSubscriber depthSubscriber = ApplicationUtils.getSpringBean(BiAnDepthSubscriber.class);
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
            BiAnTradeSubscriber tradeSubscriber = ApplicationUtils.getSpringBean(BiAnTradeSubscriber.class);
            tradeSubscribers.add(tradeSubscriber);
            List<String> registers = subList(symbolInfos, 0, groupSize);
            symbolInfos = subList(symbolInfos, groupSize, symbolInfos.size());
            tradeSubscriber.register(registers);
        }
    }

    public void wakeUpDepthAllSymbol() {
        for (String s : symbolMap.keySet()) {
            makerPool.getDepthProcessThread(s).offer(s);
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
