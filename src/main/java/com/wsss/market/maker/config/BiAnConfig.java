package com.wsss.market.maker.config;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.wsss.market.maker.depth.limit.LimitType;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@EnableApolloConfig
public class BiAnConfig {
    @Value("${binance.steam.depth.url:wss://stream.binance.com:443/stream}")
    public String binanceSteamUrl;
    /**
     * 请求币安的快照地址
     */
    @Value("${binance.rest.depth.url:https://www.binance.com/api/v1/depth}")
    public String binanceDepthUrl;
    /**
     * 请求的快照深度限制
     */
    @Value("${binance.rest.depth.limit:100}")
    public String limit;

    /**
     * 摆盘的最小间隔
     */
    @Value("${market.maker.depth.min.interval:10}")
    public long minInterval;

    @Value("${market.maker.depth.default.limit.type:ALWAYS}")
    public String limitType;

    public LimitType getLimitType() {

        return LimitType.getByName(limitType);
    }
}
