package com.wsss.market.maker.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StringUtils {
    public static Map<String,String> lowerSymbolMap = new ConcurrentHashMap<>();
    public static Map<String,String> upperSymbolMap = new ConcurrentHashMap<>();

    public static String toLowerSymbol(String symbol) {
        String lowerSymbol = lowerSymbolMap.get(symbol);
        if(lowerSymbol != null) {
            return lowerSymbol;
        }
        return lowerSymbolMap.computeIfAbsent(symbol,s->s.toLowerCase());
    }

    public static String toUpperSymbol(String symbol) {
        String upperSymbol = upperSymbolMap.get(symbol);
        if(upperSymbol != null) {
            return upperSymbol;
        }
        return upperSymbolMap.computeIfAbsent(symbol,s->s.toUpperCase());
    }
}
