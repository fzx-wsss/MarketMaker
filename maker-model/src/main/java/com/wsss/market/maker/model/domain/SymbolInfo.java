package com.wsss.market.maker.model.domain;

import com.cmcm.finance.ccc.client.CoinConfigCenterClient;
import com.cmcm.finance.ccc.client.model.SymbolAoWithFeatureAndExtra;
import com.wsss.market.maker.model.config.MakerConfig;
import com.wsss.market.maker.model.config.SymbolConfig;
import com.wsss.market.maker.model.config.TradeConfig;
import com.wsss.market.maker.model.depth.design.DepthDesignPolicy;
import com.wsss.market.maker.model.depth.design.DepthDesignType;
import com.wsss.market.maker.model.limit.MakerLimitPolicy;
import com.wsss.market.maker.model.limit.MakerLimitType;
import com.wsss.market.maker.model.trade.design.TradeDesignPolicy;
import com.wsss.market.maker.model.trade.design.TradeDesignType;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Getter
@Setter
@Component
@Scope("prototype")
public class SymbolInfo {
    private String symbol;
    private List<String> childSymbol;
    @Resource
    private CoinConfigCenterClient coinConfigCenterClient;
    @Resource
    private SymbolConfig symbolConfig;
    @Resource
    private MakerConfig makerConfig;
    @Resource
    private TradeConfig tradeConfig;

    private Map<String,BlockingQueue> subscribedQueueMap = new CacheMap<>(k->new ArrayBlockingQueue<>(1000));
    private Map<String,SubscribedOrderBook> subscribedOrderBookMap = new CacheMap<>(k-> ApplicationUtils.getSpringBean(SubscribedOrderBook.class,this));
    private volatile long lastReceiveTime;
    private volatile MakerLimitPolicy limitPolicy;
    private volatile DepthDesignPolicy depthDesignPolicy;
    private volatile TradeDesignPolicy tradeDesignPolicy;
    private OwnerOrderBook ownerOrderBook;
    private UserBBO userBBO;

    public SymbolInfo(String symbol) {
        this.symbol = symbol;
        this.ownerOrderBook = ApplicationUtils.getSpringBean(OwnerOrderBook.class,this);
    }

    @PostConstruct
    private void initChildrenSymbol() {
        SymbolAoWithFeatureAndExtra symbolAo = coinConfigCenterClient.getSymbolInfoByName(symbol);
        String mappingSymbol = symbolConfig.getMappingSymbol(symbolAo);
        String triangleSymbol = symbolConfig.getTriangleSymbol(symbolAo);
        if(mappingSymbol != null && triangleSymbol != null) {
            throw new UnsupportedOperationException("mappingSymbol and triangleSymbol only choose one");
        }
        if(mappingSymbol != null) {
            childSymbol = Arrays.asList(mappingSymbol);
            return;
        }
        if(triangleSymbol != null) {
            String ab = triangleSymbol;
            String cb = TriangleSymbol.getTriangleSymbol(triangleSymbol).getCbFromCa(symbol);
            // 严格有序
            childSymbol = Arrays.asList(cb,ab);
            return;
        }
        childSymbol = Arrays.asList(symbol);
    }

    public MakerLimitPolicy getLimitPolicy() {
        MakerLimitType makerLimitType = MakerLimitType.getByName(makerConfig.getLimitType(getSymbolAo()));
        if (limitPolicy == null || makerLimitType != limitPolicy.getLimitType()) {
            limitPolicy = makerLimitType.createMakerLimitPolicy(this);
        }
        return limitPolicy;
    }

    public DepthDesignPolicy getDepthDesignPolicy() {
//        DepthDesignType type = DepthDesignType.getByName(makerConfig.getDesignType(getSymbolAo()));
        if (depthDesignPolicy == null) {
            depthDesignPolicy = DepthDesignType.createDefaultMakerDesignPolicy(this);
        }
        return depthDesignPolicy;
    }

    public TradeDesignPolicy getTradeDesignPolicy() {
//        TradeDesignType type = TradeDesignType.getByName(tradeConfig.getDesignType(getSymbolAo()));
        if (tradeDesignPolicy == null) {
            tradeDesignPolicy = TradeDesignType.createDefaultTradeDesignPolicy(this);
        }
        return tradeDesignPolicy;
    }


    private SymbolAoWithFeatureAndExtra defaultAo = new SymbolAoWithFeatureAndExtra();
    {
        defaultAo.setShowPriceScale(6);
        defaultAo.setShowVolumeScale(4);
        defaultAo.setOffsetPriceScale(6);
        defaultAo.setOffsetVolumeScale(4);
        defaultAo.setOffsetBuyRobotId(0L);
        defaultAo.setOffsetSellRobotId(0L);
        defaultAo.setTradeMinVolume(BigDecimal.TEN);
    }
    public SymbolAoWithFeatureAndExtra getSymbolAo() {
        SymbolAoWithFeatureAndExtra symbolAo = coinConfigCenterClient.getSymbolInfoByName(symbol);
        if(symbolAo == null) {
            symbolAo = defaultAo;
        }
        return symbolAo;
    }

    public void putDepthListenTask(String childSymbol, Object depthListenTask) {
        subscribedQueueMap.get(childSymbol).offer(depthListenTask);
    }

    public SubscribedOrderBook getChildSubscribedOrderBook(String childSymbol) {
        return subscribedOrderBookMap.get(childSymbol);
    }

    public boolean isDebugLog() {
        return symbolConfig.getDebugSymbols().contains(symbol);
    }

    public boolean isMonitor() {
        return symbolConfig.getMonitorSymbols().contains(symbol);
    }
}
