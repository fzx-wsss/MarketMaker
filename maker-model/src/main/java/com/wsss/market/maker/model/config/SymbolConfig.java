package com.wsss.market.maker.model.config;

import com.cmcm.finance.ccc.client.model.SymbolAoWithFeatureAndExtra;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.Source;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class SymbolConfig {
    private static SymbolConfig cache;
    public static String LOKI_CONFIG = "lokiConfig.";
    public static Map<String, String[]> configKeyMap = new CacheMap<>(k -> k.split("\\."));
    public static Map<String, BigDecimal> bigDecimalMap = new CacheMap<>(k -> new BigDecimal(k));

    @Getter
    @ApolloJsonValue("${market.maker.support.symbols:[galaxrp]}")
    private Set<String> supportSymbols;
    @Getter
    @ApolloJsonValue("${market.maker.debug.symbols:[]}")
    private Set<String> debugSymbols;
    @ApolloJsonValue("${market.maker.mapping.symbols:{}}")
    private Map<String,String> mappingSymbols;
    /**
     * key:ca, value:ab
     */
    @ApolloJsonValue("${market.maker.triangle.symbols:{galaxrp:xrpusdt}}")
    private Map<String,String> triangleSymbols;
    @Getter
    @ApolloJsonValue("${market.maker.monitor.symbols:[btcusdt]}")
    private Set<String> monitorSymbols;

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

    @Getter
    @Value("${market.maker.subscribe.max.receive.time:300}")
    private int maxReceiveTime;

    @Value("${market.maker.subscribe.default.sub.source:Okex,Binance}")
    private Set<String> defaultSubSource;
    String SUBSCRIBE_SOURCE = SymbolConfig.LOKI_CONFIG + "subscribe";

    @Value("${market.maker.subscribe.source.price.diff:100}")
    private long priceDiff;

    public long getPriceDiff(SymbolAoWithFeatureAndExtra symbolAo) {
        return priceDiff;
    }

    public Set<Source> getSubscribeSource(SymbolAoWithFeatureAndExtra symbolAo) {
        if(symbolAo == null) {
            return Source.getSource(defaultSubSource);
        }
        String v = getJsonNodeValue(symbolAo,SUBSCRIBE_SOURCE,String.class);
        if(StringUtils.isNotEmpty(v)) {
            String[] sources = v.split(",");
            Set<Source> res = Source.getSource(sources);
            if(!res.isEmpty()) {
                return res;
            }
        }
        return Source.getSource(defaultSubSource);
    }

    public String getMappingSymbol(SymbolAoWithFeatureAndExtra symbolAo) {
        if(symbolAo == null) {
            return null;
        }
        return mappingSymbols.get(symbolAo.getSymbolName());
    }

    public String getTriangleSymbol(SymbolAoWithFeatureAndExtra symbolAo) {
        if(symbolAo == null) {
            return null;
        }
        return triangleSymbols.get(symbolAo.getSymbolName());
    }

    public static <T> T getJsonNodeValue(SymbolAoWithFeatureAndExtra symbolInfo, String key, Class<T> clazz) {
        try {
            if (symbolInfo == null || symbolInfo.getExtra() == null) {
                return null;
            }
            String[] index = configKeyMap.get(key);
            JsonNode jsonNode = symbolInfo.getExtra();
            for (String i : index) {
                jsonNode = jsonNode.get(i);
                if (jsonNode == null) {
                    return null;
                }
            }
            if (clazz == Integer.class) {
                return clazz.cast(jsonNode.asInt());
            } else if (clazz == Double.class) {
                return clazz.cast(jsonNode.asDouble());
            } else if (clazz == Long.class) {
                return clazz.cast(jsonNode.asLong());
            } else if (clazz == BigDecimal.class) {
                return clazz.cast(bigDecimalMap.get(jsonNode.asText()));
            } else if (clazz == String.class) {
                return clazz.cast(jsonNode.asText());
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public static SymbolConfig getInstance() {
        if(cache == null) {
            cache = ApplicationUtils.getSpringBean(SymbolConfig.class);
        }
        return cache;
    }

}
