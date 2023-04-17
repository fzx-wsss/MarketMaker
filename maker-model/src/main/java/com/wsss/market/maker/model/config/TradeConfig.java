package com.wsss.market.maker.model.config;

import com.wsss.market.maker.model.utils.ApplicationUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TradeConfig {
    @Getter
    @Value("${market.maker.trade.limit.interval:10}")
    private int interval;

    @Getter
    @Value("${market.maker.trade.limit.max:100}")
    private int maxLimit;

    public static TradeConfig getInstance() {
        return ApplicationUtils.getSpringBean(TradeConfig.class);
    }
}
