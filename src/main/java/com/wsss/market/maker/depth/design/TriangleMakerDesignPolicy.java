package com.wsss.market.maker.depth.design;

import com.wsss.market.maker.center.BootStrap;
import com.wsss.market.maker.config.MakerConfig;
import com.wsss.market.maker.depth.thread.DesignOrderTask;
import com.wsss.market.maker.domain.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope("prototype")
public class TriangleMakerDesignPolicy extends AbstractDesignPolicy {
    private SubscribedOrderBook cbOrderBook;
    private SubscribedOrderBook abOrderBook;
    private SubscribedOrderBook caOrderBook;

    public TriangleMakerDesignPolicy(SymbolInfo symbolInfo) {
        this.symbolInfo = symbolInfo;
        if(symbolInfo.getChildSymbol().size() != 2) {
            throw new UnsupportedOperationException("ChildSymbol size is size:" + symbolInfo.getChildSymbol().size());
        }
        String cb = symbolInfo.getChildSymbol().get(0);
        this.cbOrderBook = symbolInfo.getChildSubscribedOrderBook(cb);
        String ab = symbolInfo.getChildSymbol().get(1);
        this.abOrderBook = symbolInfo.getChildSubscribedOrderBook(ab);

        this.caOrderBook = BootStrap.getSpringBean(SubscribedOrderBook.class,symbolInfo);
    }

    @Override
    public DesignOrderTask designOrder() {
        if (cbOrderBook.buyOrSellIsEmpty() || abOrderBook.buyOrSellIsEmpty()) {
            log.warn("{} order book is empty,cb:{},ab:{}", symbolInfo.getSymbol(),cbOrderBook.size(),abOrderBook.size());
            return null;
        }
        if(!updateCaBook()) {
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
    protected SubscribedOrderBook getSubscribedOrderBook() {
        return caOrderBook;
    }


    private boolean updateCaBook() {
        // TODO: 2023/4/13 是否需要校验时间差 
        // TODO: 2023/4/13 是否需要校验价格差 
        // TODO: 2023/4/13 校验三角套利

        int priceScale = symbolInfo.getSymbolAo().getShowPriceScale();
        int volumeScale = symbolInfo.getSymbolAo().getShowVolumeScale();

        List<BigDecimal> abBuys = new ArrayList<>(abOrderBook.getBuyPrices());
        List<BigDecimal> cbSells = new ArrayList<>(cbOrderBook.getSellPrices());
        int min = Math.min(abBuys.size(), cbSells.size());
        for (int i = 0; i < min; i++) {
            BigDecimal abPrice = abBuys.get(i);
            BigDecimal cbPrice = cbSells.get(i);
            if (abPrice.compareTo(BigDecimal.ZERO) > 0) {
                // BigDecimal abVolume = Fmt.dec(abBook.getBidSize(abBidPrices[i]), ab.getSizeFractionDigits());
                BigDecimal caPrice = cbPrice.divide(abPrice, priceScale, BigDecimal.ROUND_CEILING);
                BigDecimal cbVolume = cbOrderBook.getSellBook(cbPrice).getVolume().setScale(volumeScale,BigDecimal.ROUND_CEILING);
                if (caPrice.compareTo(BigDecimal.ZERO) > 0) {
                    caOrderBook.update(Side.SELL, caPrice, cbVolume, Source.BITURE);
                }
            }
        }

        List<BigDecimal> abSells = new ArrayList<>(abOrderBook.getSellPrices());
        List<BigDecimal> cbBuys = new ArrayList<>(cbOrderBook.getBuyPrices());
        min = Math.min(abSells.size(), cbBuys.size());
        for (int i = 0; i < min; i++) {
            BigDecimal abPrice = abSells.get(i);
            BigDecimal cbPrice = cbBuys.get(i);
            if (abPrice.compareTo(BigDecimal.ZERO) > 0) {
                // BigDecimal abVolume = Fmt.dec(abBook.getBidSize(abBidPrices[i]), ab.getSizeFractionDigits());
                BigDecimal caPrice = cbPrice.divide(abPrice, priceScale, BigDecimal.ROUND_CEILING);
                BigDecimal cbVolume = cbOrderBook.getBuyBook(cbPrice).getVolume().setScale(volumeScale,BigDecimal.ROUND_CEILING);
                if (caPrice.compareTo(BigDecimal.ZERO) > 0) {
                    caOrderBook.update(Side.BUY, caPrice, cbVolume, Source.BITURE);
                }
            }
        }
        return true;
    }

    @Override
    public DesignType getDesignType() {
        return DesignType.TRIANGLE;
    }
}
