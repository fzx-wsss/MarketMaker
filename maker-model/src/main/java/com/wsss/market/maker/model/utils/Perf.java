package com.wsss.market.maker.model.utils;

import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;

public class Perf {

    public static void count(String key) {
        Monitor.counter(key).end();
    }

    private static CacheMap<String,CacheMap<String, Monitor.CountContext>> nameMap = new CacheMap<>(k->new CacheMap<>(key->Monitor.counter(k+"_"+key)));
    public static void count(String key, SymbolInfo symbolInfo) {
        count(key,symbolInfo,1D);
    }

    public static void count(String key, SymbolInfo symbolInfo,double num) {
        Monitor.counter(key).end();
        if(symbolInfo.isMonitor()) {
            nameMap.get(key).get(symbolInfo.getSymbol()).end(num);
        }
    }

    private static CacheMap<String,CacheMap<String, Monitor.CountContext>> keyMap = new CacheMap<>(k->new CacheMap<>(key->Monitor.counter(k+"_"+key)));
    public static void count(String key, String key2) {
        nameMap.get(key).get(key2).end();
    }


}
