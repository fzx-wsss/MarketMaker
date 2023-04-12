package com.wsss.market.maker.config;

import com.cmcm.finance.ccc.client.model.SymbolAoWithFeatureAndExtra;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.wsss.market.maker.center.BootStrap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SymbolConfig {
    @ApolloJsonValue("${market.maker.mapping.symbols:{\"xrpusdt\":\"btcusdt\"}}")
    private Map<String,String> mappingSymbols;
    @ApolloJsonValue("${market.maker.triangle.symbols:{}}")
    private Map<String,String> triangleSymbols;

    @Getter
    @Value("${market.maker.subscribed.group.size:20}")
    private int groupSize;

    public String getMappingSymbol(SymbolAoWithFeatureAndExtra symbolAo) {
        return mappingSymbols.get(symbolAo.getSymbolName());
    }

    public String getTriangleSymbol(SymbolAoWithFeatureAndExtra symbolAo) {
        return triangleSymbols.get(symbolAo.getSymbolName());
    }

    public static SymbolConfig getInstance() {
        return BootStrap.getSpringBean(SymbolConfig.class);
    }
}
