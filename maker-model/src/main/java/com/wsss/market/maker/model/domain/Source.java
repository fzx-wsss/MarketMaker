package com.wsss.market.maker.model.domain;

import java.util.*;

public enum Source {
    Binance("com.wsss.market.maker.service.subscribe.bian.BiAnDepthSubscriber",
           "com.wsss.market.maker.service.subscribe.bian.BiAnTradeSubscriber"),
    Okex("com.wsss.market.maker.service.subscribe.ok.OkDepthSubscriber",
            "com.wsss.market.maker.service.subscribe.ok.OkTradeSubscriber"),
    Bybit("com.wsss.market.maker.service.subscribe.bybit.BybitDepthSubscriber",
            "com.wsss.market.maker.service.subscribe.bybit.BybitTradeSubscriber"),
    Huobi,
    CoinbasePro,
    Bitrue,
    Bitmax,
    Hitbtc,
    Bigone,
    GateIo,
    Bittrex;

    private String depthSubscriber;
    private String tradeSubscriber;

    Source() {
    }

    Source(String depthSubscriber, String tradeSubscriber) {
        this.depthSubscriber = depthSubscriber;
        this.tradeSubscriber = tradeSubscriber;
    }

    public Class getDepthSubscriber() {
        try {
            return Class.forName(this.depthSubscriber);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public Class getTradeSubscriber() {
        try {
            return Class.forName(this.tradeSubscriber);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String,Source> cache = new CacheMap<>(k->{
        for (Source value : Source.values()) {
            if(value.name().equalsIgnoreCase(k)) {
                return value;
            }
        }
        return null;
    });

    public static Source getSource(String name) {
        return cache.get(name);
    }

    public static Set<Source> getSource(String... name) {
        return getSource(Arrays.asList(name));
    }
    public static Set<Source> getSource(Collection<String> name) {
        Set<Source> res = new HashSet<>();
        for (String source : name) {
            if(Source.getSource(source) != null) {
                res.add(Source.getSource(source));
            }
        }
        if(!res.isEmpty()) {
            return res;
        }
        return Collections.EMPTY_SET;
    }
}
