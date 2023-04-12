package com.wsss.market.maker.config;

import com.cmcm.finance.ccc.client.CoinConfigCenterClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: Yoruichi
 * @Date: 2019/8/9 6:25 PM
 */
@Configuration
public class CoinConfigCenterConfig {
    @Value("${system.ccc.url:http://api.bitrue.com/coin-info/v3/rawCoinSymbolInfo.json}")
    private String cccUrl;
    @Value("${system.ccc.freshTime:30}")
    private long freshTime;

    @Bean
    public CoinConfigCenterClient getCCCClient() {
        CoinConfigCenterClient client = new CoinConfigCenterClient();
        client.setRefreshInterval(freshTime);
        client.setUrl(cccUrl);
        return client;
    }
}
