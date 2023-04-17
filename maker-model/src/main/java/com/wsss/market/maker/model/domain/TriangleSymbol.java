package com.wsss.market.maker.model.domain;

import lombok.Getter;

@Getter
public enum TriangleSymbol {
    XRP_USDT("xrpusdt","xrp","usdt"),
    XRP_ETH("xrpeth","xrp","eth"),
    XRP_BTC("xrpbtc","xrp","btc");

    private String symbol;
    private String baseCoin;
    private String quoteCoin;

    TriangleSymbol(String symbol, String baseCoin, String quoteCoin) {
        this.symbol = symbol;
        this.baseCoin = baseCoin;
        this.quoteCoin = quoteCoin;
    }

    // ab: xrpusdt, ca: bnbxrp -> cb: bnbusdt
    public String getCbFromCa(String ca) {
        return ca.substring(0, ca.lastIndexOf(this.getBaseCoin())) + this.getQuoteCoin();
    }

    private static CacheMap<String,TriangleSymbol> cacheMap = new CacheMap<>(k-> {
        for(TriangleSymbol ts : values()) {
            if(ts.getSymbol().equals(k.toLowerCase())) {
                return ts;
            }
        }
        throw new UnsupportedOperationException("TriangleSymbol illegal:" + k);
    });

    public static TriangleSymbol getTriangleSymbol(String symbol) {
        return cacheMap.get(symbol);
    }
}
