package com.wsss.market.maker.model.utils;

import com.wsss.market.maker.model.domain.CacheMap;

import java.math.BigDecimal;

public class BigDecimalUtils {
    private static final CacheMap<?,BigDecimal> CACHE_MAP = new CacheMap<>(k->new BigDecimal(k.toString()));
    public static final BigDecimal WAN = new BigDecimal("10000");
    public static final BigDecimal QIAN = new BigDecimal("1000");
    public static final BigDecimal ER = new BigDecimal("2");

    public static final BigDecimal convert(Object key) {
        return CACHE_MAP.get(key);
    }
}
