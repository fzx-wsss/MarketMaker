package com.wsss.market.maker.depth.design;

import com.wsss.market.maker.config.MakerConfig;
import com.wsss.market.maker.depth.thread.DesignOrderTask;
import com.wsss.market.maker.domain.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope("prototype")
public class FollowMakerDesignPolicy extends AbstractDesignPolicy {
    private SubscribedOrderBook subscribedOrderBook;

    public FollowMakerDesignPolicy(SymbolInfo symbolInfo) {
        this.symbolInfo = symbolInfo;
        if(symbolInfo.getChildSymbol().size() != 1) {
            throw new UnsupportedOperationException("ChildSymbol size is size:" + symbolInfo.getChildSymbol().size());
        }
        String mappingSymbol = symbolInfo.getChildSymbol().get(0);
        this.subscribedOrderBook = symbolInfo.getChildSubscribedOrderBook(mappingSymbol);
    }

    @Override
    public DesignOrderTask designOrder() {
        if (subscribedOrderBook.buyOrSellIsEmpty()) {
            log.warn("{} order book is empty", symbolInfo.getSymbol());
            return null;
        }
        MakerContext makerContext = getMakerContext();
        if (makerContext == null) {
            return null;
        }
        avoidOrEatUserOrder(makerContext);
        followPlaceOrCancelOrder(makerContext);
        return DesignOrderTask.builder()
                .symbolInfo(symbolInfo)
                .placeOrderList(makerContext.getPlaceOrders())
                .cancelOrderList(makerContext.getRemoveOrders())
                .build();
    }

    @Override
    public DesignType getDesignType() {
        return DesignType.FOLLOW;
    }


    @Override
    protected SubscribedOrderBook getSubscribedOrderBook() {
        return subscribedOrderBook;
    }

}
