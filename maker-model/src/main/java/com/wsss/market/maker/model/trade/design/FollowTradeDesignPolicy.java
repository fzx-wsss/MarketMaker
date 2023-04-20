package com.wsss.market.maker.model.trade.design;

import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.Trade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Scope("prototype")
public class FollowTradeDesignPolicy extends AbstractTradeDesignPolicy {

    public FollowTradeDesignPolicy(SymbolInfo symbolInfo) {
        super(symbolInfo);
        if(symbolInfo.getChildSymbol().size() != 1) {
            throw new UnsupportedOperationException("ChildSymbol size is size:" + symbolInfo.getChildSymbol().size());
        }
    }

    @Override
    public Trade designTrade(Trade trade) {
        BigDecimal volume = getFixedVolume(trade.getVolume());
        if(volume == null) {
            return null;
        }
        BigDecimal price = getFixedPrice(trade.getPrice());
        trade.setVolume(volume);
        trade.setPrice(price);
        return trade;
    }

    @Override
    public TradeDesignType getDesignType() {
        return null;
    }
}
