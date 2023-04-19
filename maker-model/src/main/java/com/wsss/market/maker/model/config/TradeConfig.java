package com.wsss.market.maker.model.config;

import com.cmcm.finance.ccc.client.model.SymbolAoWithFeatureAndExtra;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class TradeConfig {
    private static TradeConfig cache;
    @Getter
    @Value("${market.maker.trade.limit.interval:10}")
    private int interval;

    @Getter
    @Value("${market.maker.trade.limit.max:100}")
    private int maxLimit;

    @Getter
    @Value("${market.maker.trade.limit.max:100}")
    private int sleep;

    @Value("${market.maker.trade.price.discount:0}")
    private String priceStrategy;

    /**
     * 成交的价格浮动
     */
    private String PRICE_STRATEGY = SymbolConfig.LOKI_CONFIG + "pd";

    @Value("${market.maker.trade.volume.discount:0.1}")
    private String volumeStrategy;
    @Value("${market.maker.trade.volume.discount.random:0.01}")
    private String volumeRandom;
    /**
     * 成交的数量浮动
     */
    private String TRADE_VOLUME_DISCOUNT_KEY = "trade_volume_discount";

    public BigDecimal getPriceStrategy(SymbolAoWithFeatureAndExtra symbolAo) {
        BigDecimal discount = SymbolConfig.getJsonNodeValue(symbolAo, PRICE_STRATEGY, BigDecimal.class);
        if (discount != null) {
            return discount;
        }
        return SymbolConfig.bigDecimalMap.get(priceStrategy);
    }

    public BigDecimal getVolumeStrategy(SymbolAoWithFeatureAndExtra symbolAo) {
        BigDecimal discount = SymbolConfig.getJsonNodeValue(symbolAo, TRADE_VOLUME_DISCOUNT_KEY, BigDecimal.class);
        if (discount != null) {
            return discount;
        }
        return SymbolConfig.bigDecimalMap.get(volumeStrategy);
    }
    public BigDecimal getVolumeRandom(SymbolAoWithFeatureAndExtra symbolAo) {
        return SymbolConfig.bigDecimalMap.get(volumeRandom);
    }

    public static TradeConfig getInstance() {
        if(cache == null) {
            cache = ApplicationUtils.getSpringBean(TradeConfig.class);
        }
        return cache;
    }
}
