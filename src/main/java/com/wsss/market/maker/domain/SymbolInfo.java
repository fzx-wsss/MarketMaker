package com.wsss.market.maker.domain;

import com.cmcm.finance.ccc.client.CoinConfigCenterClient;
import com.cmcm.finance.ccc.client.model.SymbolAoWithFeatureAndExtra;
import com.wsss.market.maker.center.BootStrap;
import com.wsss.market.maker.config.MakerConfig;
import com.wsss.market.maker.config.SymbolConfig;
import com.wsss.market.maker.depth.design.DesignType;
import com.wsss.market.maker.depth.limit.LimitType;
import com.wsss.market.maker.depth.subscribe.DepthListenTask;
import com.wsss.market.maker.depth.thread.DesignOrderTask;
import com.wsss.market.maker.depth.design.MakerDesignPolicy;
import com.wsss.market.maker.depth.limit.MakerLimitPolicy;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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

    private Map<String,BlockingQueue<DepthListenTask>> subscribedQueueMap = new CacheMap<>(k->new ArrayBlockingQueue<>(1000));
    private Map<String,SubscribedOrderBook> subscribedOrderBookMap = new CacheMap<>(k->BootStrap.getSpringBean(SubscribedOrderBook.class,this));
    private volatile long lastReceiveTime;
    private volatile MakerLimitPolicy limitPolicy;
    private volatile MakerDesignPolicy designPolicy;
    private volatile DesignOrderTask designOrderTasks;
    private OwnerOrderBook ownerOrderBook;
    private UserBBO userBBO;

    public SymbolInfo(SymbolAoWithFeatureAndExtra symbolAo) {
        this.symbol = symbolAo.getSymbolName();
        this.ownerOrderBook = BootStrap.getSpringBean(OwnerOrderBook.class,this);
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
        LimitType limitType = LimitType.getByName(makerConfig.getLimitType(getSymbolAo()));
        if (limitPolicy == null || limitType != limitPolicy.getLimitType()) {
            limitPolicy = limitType.createMakerLimitPolicy(this);
        }
        return limitPolicy;
    }

    public MakerDesignPolicy getDesignPolicy() {
        DesignType type = DesignType.getByName(makerConfig.getDesignType(getSymbolAo()));
        if (designPolicy == null || type != designPolicy.getDesignType()) {
            designPolicy = type.createMakerDesignPolicy(this);
        }
        return designPolicy;
    }

    public void addDesignOrderTask(DesignOrderTask task) {
        designOrderTasks = task;
    }

    /**
     * 上次任务是否结束
     *
     * @return
     */
    public boolean isAllDesignOrderTasksFinished() {
        if (designOrderTasks == null) {
            return true;
        }
        return designOrderTasks.isFinish();
    }

    public SymbolAoWithFeatureAndExtra getSymbolAo() {
        return coinConfigCenterClient.getSymbolInfoByName(symbol);
    }

    public void putDepthListenTask(String childSymbol, DepthListenTask depthListenTask) {
        subscribedQueueMap.get(childSymbol).offer(depthListenTask);
    }

    public SubscribedOrderBook getChildSubscribedOrderBook(String childSymbol) {
        return subscribedOrderBookMap.get(childSymbol);
    }

    public boolean isDebugLog() {
        return symbolConfig.getDebugSymbols().contains(symbol);
    }
}
