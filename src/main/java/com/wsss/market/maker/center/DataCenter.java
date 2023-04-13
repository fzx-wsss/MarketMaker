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

    public void register(List<SymbolAoWithFeatureAndExtra> symbolAos) {
        List<String> symbolNames = symbolAos.stream().map(s->s.getSymbolName()).collect(Collectors.toList());
        log.info("register symbol:{}", symbolNames);
        List<SymbolInfo> symbolInfos = symbolAos.stream().map(s->{
            SymbolInfo symbolInfo = symbolMap.computeIfAbsent(s.getSymbolName(), k -> BootStrap.getSpringBean(SymbolInfo.class, s));
            for (String child : symbolInfo.getChildSymbol()) {
                mappingMap.computeIfAbsent(child, k -> Sets.newConcurrentHashSet()).add(symbolInfo);
            }
            return symbolInfo;
        }).collect(Collectors.toList());


        registerDepth(symbolInfos);
        registerTrade(symbolInfos);
    }

    private void registerDepth(List<SymbolInfo> symbolInfos) {
        int groupSize = symbolConfig.getGroupSize();
        while (!symbolInfos.isEmpty()) {
            for (BiAnDepthSubscriber subscriber : depthSubscribers) {
                int available = groupSize - subscriber.getSubscribedSymbol().size();
                if (available <= 0) {
                    continue;
                }
                List<SymbolInfo> registers = subList(symbolInfos, 0, available);
                symbolInfos = subList(symbolInfos, available, symbolInfos.size());
                subscriber.register(registers);
            }
            break;
        }
        while (!symbolInfos.isEmpty()) {
            BiAnDepthSubscriber depthSubscriber = BootStrap.getSpringBean(BiAnDepthSubscriber.class);
            depthSubscribers.add(depthSubscriber);
            List<SymbolInfo> registers = subList(symbolInfos, 0, groupSize);
            symbolInfos = subList(symbolInfos, groupSize, symbolInfos.size());
            depthSubscriber.register(registers);
        }
    }

    private List<SymbolInfo> subList(List<SymbolInfo> list, int start, int end) {
        if (start >= list.size()) {
            return Collections.EMPTY_LIST;
        }
        if (end > list.size()) {
            return list;
        }
        return list.subList(start, end);
    }



    private void registerTrade(List<SymbolInfo> symbolInfos) {
        int groupSize = symbolConfig.getGroupSize();

        while (!symbolInfos.isEmpty()) {
            for (BiAnTradeSubscriber subscriber : tradeSubscribers) {
                int available = groupSize - subscriber.getSubscribedSymbol().size();
                if (available <= 0) {
                    continue;
                }
                List<SymbolInfo> registers = subList(symbolInfos, 0, available);
                symbolInfos = subList(symbolInfos, available, symbolInfos.size());
                subscriber.register(registers);
            }
            break;
        }
        while (!symbolInfos.isEmpty()) {
            BiAnTradeSubscriber tradeSubscriber = BootStrap.getSpringBean(BiAnTradeSubscriber.class);
            tradeSubscribers.add(tradeSubscriber);
            List<SymbolInfo> registers = subList(symbolInfos, 0, groupSize);
            symbolInfos = subList(symbolInfos, groupSize, symbolInfos.size());
            tradeSubscriber.register(registers);
        }
    }

    public Set<SymbolInfo> getMappingSymbolInfo(String symbol) {
        return mappingMap.get(symbol);
    }
}
