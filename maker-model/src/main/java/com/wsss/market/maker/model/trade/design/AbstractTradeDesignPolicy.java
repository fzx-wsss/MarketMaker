package com.wsss.market.maker.model.trade.design;

import com.wsss.market.maker.model.config.TradeConfig;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.Perf;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public abstract class AbstractTradeDesignPolicy implements TradeDesignPolicy {
    protected SymbolInfo symbolInfo;

    public AbstractTradeDesignPolicy(SymbolInfo symbolInfo) {
        this.symbolInfo = symbolInfo;
    }

    protected BigDecimal getFixedPrice(BigDecimal originalPrice) {
        TradeConfig tradeConfig = TradeConfig.getInstance();
        BigDecimal strategy = tradeConfig.getPriceStrategy(symbolInfo.getSymbolAo());
        if(BigDecimal.ZERO.compareTo(strategy) == 0) {
            return originalPrice;
        }
        BigDecimal discount = ThreadLocalRandom.current().nextBoolean() ? BigDecimal.ONE.add(strategy) : BigDecimal.ONE.subtract(strategy);
        BigDecimal fixedPrice = originalPrice.multiply(discount).setScale(symbolInfo.getSymbolAo().getShowPriceScale(), BigDecimal.ROUND_DOWN);
        if(fixedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("{} fixed price less zero,strategy:{},",symbolInfo.getSymbol());
            return originalPrice;
        }
        return fixedPrice;
    }

    protected BigDecimal getFixedVolume(BigDecimal originalVolume) {
        TradeConfig tradeConfig = TradeConfig.getInstance();
        BigDecimal discount = tradeConfig.getVolumeStrategy(symbolInfo.getSymbolAo());
        BigDecimal random = tradeConfig.getVolumeRandom(symbolInfo.getSymbolAo());
        random = ThreadLocalRandom.current().nextBoolean() ? BigDecimal.ONE.add(random) : BigDecimal.ONE.subtract(random);
        BigDecimal fixedVolume = originalVolume.multiply(discount).multiply(random).setScale(symbolInfo.getSymbolAo().getShowVolumeScale(), BigDecimal.ROUND_DOWN);
        if (fixedVolume.compareTo(symbolInfo.getSymbolAo().getTradeMinVolume()) < 0) {
            Perf.count("volume_too_small",symbolInfo);
            return null;
        }
        return fixedVolume;
    }
}
