package com.wsss.market.maker.model.trade.design;

import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.SubscribedOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.Trade;
import com.wsss.market.maker.model.utils.BigDecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Scope("prototype")
public class TriangleTradeDesignPolicy extends AbstractTradeDesignPolicy {
    private SubscribedOrderBook abOrderBook;
    private String cbSymbol;


    public TriangleTradeDesignPolicy(SymbolInfo symbolInfo) {
        super(symbolInfo);
        if(symbolInfo.getChildSymbol().size() != 2) {
            throw new UnsupportedOperationException("ChildSymbol size is size:" + symbolInfo.getChildSymbol().size());
        }
        this.cbSymbol = symbolInfo.getChildSymbol().get(0);
        this.abOrderBook = symbolInfo.getChildSubscribedOrderBook(symbolInfo.getChildSymbol().get(1));
    }

    @Override
    public Trade designTrade(Trade trade) {
        if(!cbSymbol.equals(trade.getSymbol())
                || abOrderBook.getBestBuy() == null
                || abOrderBook.getBestSell() == null) {
            return null;
        }
        int priceScale = symbolInfo.getSymbolAo().getShowPriceScale();
        int volumeScale = symbolInfo.getSymbolAo().getShowVolumeScale();
        int round = trade.getTrendSide() == Side.BUY ? BigDecimal.ROUND_FLOOR : BigDecimal.ROUND_CEILING;
        BigDecimal abPrice = abOrderBook.getBestBuy().add(abOrderBook.getBestSell()).divide(BigDecimalUtils.ER,priceScale,round);
        BigDecimal cbPrice = trade.getPrice();

        BigDecimal caPrice = cbPrice.divide(abPrice, priceScale,round);
        caPrice = getFixedPrice(caPrice);
        BigDecimal caVolume = trade.getVolume().divide(caPrice,volumeScale,round);
        caVolume = getFixedVolume(caVolume);
        if(caVolume == null) {
            return null;
        }
        trade.setVolume(caVolume);
        trade.setPrice(caPrice);
        trade.setSymbol(symbolInfo.getSymbol());
        return trade;
    }

    @Override
    public TradeDesignType getDesignType() {
        return null;
    }
}
