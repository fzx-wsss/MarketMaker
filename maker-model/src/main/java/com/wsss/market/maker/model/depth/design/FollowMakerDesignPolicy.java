package com.wsss.market.maker.model.depth.design;

import com.wsss.market.maker.model.domain.SubscribedOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
    public MakerContext designOrder() {
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
        return makerContext;
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
