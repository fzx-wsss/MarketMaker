package com.wsss.market.maker.model.config;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@EnableApolloConfig
public class SourceConfig {
    private static SourceConfig cache;
    @Value("${binance.steam.depth.url:wss://stream.binance.com:443/stream}")
    private String binanceSteamUrl;
    /**
     * 请求币安的快照地址
     */
    @Value("${binance.rest.depth.url:https://www.binance.com/api/v1/depth}")
    private String binanceDepthUrl;
    /**
     * 请求的快照深度限制
     */
    @Value("${binance.rest.depth.limit:100}")
    private String limit;


    @Value("${ok.steam.depth.url:wss://wsaws.okx.com:8443/ws/v5/public}")
    private String okSteamUrl;

    public static SourceConfig getInstance() {
        if(cache == null) {
            cache = ApplicationUtils.getSpringBean(SourceConfig.class);
        }
        return cache;
    }
}
