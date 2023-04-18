package com.wsss.market.maker.model.config;

import com.cmcm.finance.ccc.client.model.SymbolAoWithFeatureAndExtra;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class SymbolConfig {
    @Getter
    @ApolloJsonValue("${market.maker.support.symbols:[\"btcusdt\",\"ethusdt\",\"xrpusdt\",\"etcbtc\",\"bchbtc\",\"ltcbtc\"]}")
    private Set<String> supportSymbols;
    @Getter
    @ApolloJsonValue("${market.maker.debug.symbols:[etcbtc]}")
    private Set<String> debugSymbols;
    @ApolloJsonValue("${market.maker.mapping.symbols:{}}")
    private Map<String,String> mappingSymbols;
    @ApolloJsonValue("${market.maker.triangle.symbols:{}}")
    private Map<String,String> triangleSymbols;

    @Getter
    @Value("${market.maker.subscribed.group.size:20}")
    private int groupSize;

    @Getter
    @Value("${market.maker.symbol.hash.start:0}")
    private int startHash;
    @Getter
    @Value("${market.maker.symbol.hash.end:10000}")
    private int endHash;

    @Getter
    @Value("${market.maker.symbol.reload.time:30}")
    private int reloadTime;

    public String getMappingSymbol(SymbolAoWithFeatureAndExtra symbolAo) {
        return mappingSymbols.get(symbolAo.getSymbolName());
    }

    public String getTriangleSymbol(SymbolAoWithFeatureAndExtra symbolAo) {
        return triangleSymbols.get(symbolAo.getSymbolName());
    }

    public static SymbolConfig getInstance() {
        return ApplicationUtils.getSpringBean(SymbolConfig.class);
    }

}
