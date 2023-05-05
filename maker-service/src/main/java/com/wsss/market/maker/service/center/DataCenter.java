package com.wsss.market.maker.service.center;

import com.wsss.market.maker.model.config.SymbolConfig;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.Source;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.service.subscribe.Subscriber;
import com.wsss.market.maker.service.task.CancelOwnerOrderTask;
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
    private SymbolConfig symbolConfig;
    @Resource
    private MarkerMakerThreadPool makerPool;

    /**
     * 实际币对名称与实际币对的关系
     */
    private Map<String, SymbolInfo> symbolMap = new ConcurrentHashMap<>();
    /**
     * 映射的币对名称与实际币对的关系
     *
     */
    private Map<Source,Map</*子币对*/String, Set<SymbolInfo>>> mappingMap = new ConcurrentHashMap<>();

    private Map<Source,List<Subscriber>> depthSubscriberMap = new CacheMap<>(k->new ArrayList<>());
    private Map<Source,List<Subscriber>> tradeSubscriberMap = new CacheMap<>(k->new ArrayList<>());

    public SymbolInfo getSymbolInfo(String symbol) {
        return symbolMap.get(symbol);
    }

    public synchronized void register(Set<String> symbolNames) {
        log.info("register symbol:{}", symbolNames);
        Set<SymbolInfo> addSymbolInfo = createSymbolInfo(symbolNames);
        Map<Source,List<String>> addChildSymbolMap = addSubscribe(addSymbolInfo);

        registerDepth(addChildSymbolMap);
        registerTrade(addChildSymbolMap);
    }

    private Set<SymbolInfo> createSymbolInfo(Set<String> symbolNames) {
        Set<SymbolInfo> set = new HashSet<>();
        List<Future> syncTaskFuture = new ArrayList<>();

        symbolNames.stream().forEach(s->{
            if(symbolMap.containsKey(s)) {
                return;
            }
            SymbolInfo symbolInfo = ApplicationUtils.getSpringBean(SymbolInfo.class, s);
            symbolMap.put(s, symbolInfo);
            QueryOwnerOrderTask orderTask = QueryOwnerOrderTask.builder().symbol(symbolInfo).build();
            syncTaskFuture.add(makerPool.execAsyncTask(orderTask));
            set.add(symbolInfo);
        });

        syncTaskFuture.forEach(f-> {
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return set;
    }

    private Map<Source,List<String>> addSubscribe(Set<SymbolInfo> symbolInfoSet) {
        CacheMap<Source,List<String>> childMap = new CacheMap<>(k->new ArrayList<>());
        for (SymbolInfo symbolInfo : symbolInfoSet) {
            Set<Source> sources = symbolConfig.getSubscribeSource(symbolInfo.getSymbolAo());
            for (Source source : sources) {
                Map</*子币对*/String, Set<SymbolInfo>> map = mappingMap.computeIfAbsent(source, k -> new ConcurrentHashMap<>());
                for (String child : symbolInfo.getChildSymbol()) {
                    Set<SymbolInfo> set = map.computeIfAbsent(child,k->new HashSet<>());
                    if(set.isEmpty()) {
                        childMap.get(source).add(child);
                    }
                    set.add(symbolInfo);
                }
            }
        }
        return childMap;
    }

    public synchronized void remove(Set<String> symbolNames) {
        log.info("remove symbol:{}", symbolNames);
        Set<SymbolInfo> symbolInfos = removeSymbolInfo(symbolNames);
        Map<Source,List<String>> removeChildSymbolMap = removeSubscribe(symbolInfos);

        removeDepth(removeChildSymbolMap);
        removeTrade(removeChildSymbolMap);

        symbolInfos.forEach(s->{
            CancelOwnerOrderTask orderTask = CancelOwnerOrderTask.builder().symbolInfo(s).build();
            makerPool.execAsyncTask(orderTask);
        });
    }

    private Set<SymbolInfo> removeSymbolInfo(Set<String> symbolNames) {
        Set<SymbolInfo> symbolInfos = new HashSet<>();
        symbolNames.forEach(s -> {
            if (!symbolMap.containsKey(s)) {
                return;
            }
            SymbolInfo symbolInfo = symbolMap.remove(s);
            symbolInfos.add(symbolInfo);
        });
        return symbolInfos;
    }

    private Map<Source,List<String>> removeSubscribe(Set<SymbolInfo> symbolInfoSet) {
        CacheMap<Source,List<String>> childMap = new CacheMap<>(k->new ArrayList<>());
        for (SymbolInfo symbolInfo : symbolInfoSet) {
            for (Map.Entry<Source, Map<String, Set<SymbolInfo>>> sourceMapEntry : mappingMap.entrySet()) {
                Source source = sourceMapEntry.getKey();
                for (Map.Entry<String, Set<SymbolInfo>> stringSetEntry : sourceMapEntry.getValue().entrySet()) {
                    String childSymbol = stringSetEntry.getKey();
                    Set<SymbolInfo> set = stringSetEntry.getValue();
                    if(set.contains(symbolInfo)) {
                        set.remove(symbolInfo);
                        if(set.isEmpty()) {
                            childMap.get(source).add(childSymbol);
                        }
                    }
                }
            }
        }
        return childMap;
    }

    private void removeDepth(Map<Source,List<String>> childSymbolMap) {
        childSymbolMap.forEach((source,symbolInfos)-> {
            List<Subscriber> depthSubscribers = depthSubscriberMap.get(source);
            Iterator<Subscriber> iterator = depthSubscribers.iterator();
            while(iterator.hasNext()) {
                Subscriber subscriber = iterator.next();
                subscriber.remove(new HashSet<>(symbolInfos));
                if(subscriber.getSubscribedSymbol().isEmpty()) {
                    log.info("关闭深度订阅");
                    subscriber.close();
                    iterator.remove();
                }
            }
        });
    }

    private void removeTrade(Map<Source,List<String>> childSymbolMap) {
        childSymbolMap.forEach((source,symbolInfos)-> {
            List<Subscriber> tradeSubscribers = tradeSubscriberMap.get(source);
            Iterator<Subscriber> iterator = tradeSubscribers.iterator();
            while(iterator.hasNext()) {
                Subscriber subscriber = iterator.next();
                subscriber.remove(new HashSet<>(symbolInfos));
                if(subscriber.getSubscribedSymbol().isEmpty()) {
                    log.info("关闭成交订阅");
                    subscriber.close();
                    iterator.remove();
                }
            }
        });
    }

    private void registerDepth(Map<Source,List<String>> childSymbolMap) {
        log.info("首次注册深度:{}",childSymbolMap);
        int groupSize = symbolConfig.getGroupSize();
        childSymbolMap.forEach((source,symbols)->{
            List<String> symbolInfos = new ArrayList<>(symbols);
            List<Subscriber> depthSubscribers = depthSubscriberMap.get(source);
            while (!symbolInfos.isEmpty()) {
                for (Subscriber subscriber : depthSubscribers) {
                    int available = groupSize - subscriber.getSubscribedSymbol().size();
                    if (available <= 0) {
                        continue;
                    }
                    List<String> registers = subList(symbolInfos, 0, available);
                    symbolInfos = subList(symbolInfos, available, symbolInfos.size());
                    subscriber.register(new HashSet<>(registers));
                }
                break;
            }
            while (!symbolInfos.isEmpty()) {
                Subscriber depthSubscriber = (Subscriber) ApplicationUtils.getSpringBean(source.getDepthSubscriber());
                depthSubscriber.init();
                depthSubscribers.add(depthSubscriber);
                List<String> registers = subList(symbolInfos, 0, groupSize);
                symbolInfos = subList(symbolInfos, groupSize, symbolInfos.size());
                depthSubscriber.register(new HashSet<>(registers));
            }
        });

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



    private void registerTrade(Map<Source,List<String>> childSymbolMap) {
        log.info("首次注册成交:{}",childSymbolMap);
        int groupSize = symbolConfig.getGroupSize();
        childSymbolMap.forEach((source,symbols)-> {
            List<String> symbolInfos = new ArrayList<>(symbols);
            List<Subscriber> tradeSubscribers = tradeSubscriberMap.get(source);
            while (!symbolInfos.isEmpty()) {
                for (Subscriber subscriber : tradeSubscribers) {
                    int available = groupSize - subscriber.getSubscribedSymbol().size();
                    if (available <= 0) {
                        continue;
                    }
                    List<String> registers = subList(symbolInfos, 0, available);
                    symbolInfos = subList(symbolInfos, available, symbolInfos.size());
                    subscriber.register(new HashSet<>(registers));
                }
                break;
            }
            while (!symbolInfos.isEmpty()) {
                Subscriber tradeSubscriber = (Subscriber) ApplicationUtils.getSpringBean(source.getTradeSubscriber());
                tradeSubscriber.init();
                tradeSubscribers.add(tradeSubscriber);
                List<String> registers = subList(symbolInfos, 0, groupSize);
                symbolInfos = subList(symbolInfos, groupSize, symbolInfos.size());
                tradeSubscriber.register(new HashSet<>(registers));
            }
        });

    }

    public void wakeUpDepthAllSymbol() {
        for (String s : symbolMap.keySet()) {
            makerPool.getDepthProcessThread(s).offer(s);
        }
    }

    public void checkSubscriber() {
        for (List<Subscriber> value : depthSubscriberMap.values()) {
            for (Subscriber subscriber : value) {
                subscriber.checkSelf();
            }
        }
        for (List<Subscriber> value : tradeSubscriberMap.values()) {
            for (Subscriber subscriber : value) {
                subscriber.checkSelf();
            }
        }
    }

    public Set<SymbolInfo> getMappingSymbolInfo(Source source, String symbol) {
        Map<String,Set<SymbolInfo>> map = mappingMap.getOrDefault(source,Collections.EMPTY_MAP);
        return map.getOrDefault(symbol,Collections.EMPTY_SET);
    }

    public boolean isRegistered(String symbol) {
        return symbolMap.containsKey(symbol);
    }

    public Set<String> registeredSymbols() {
        return symbolMap.keySet();
    }
}
