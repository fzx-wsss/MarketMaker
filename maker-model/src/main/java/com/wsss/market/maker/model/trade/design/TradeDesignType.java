package com.wsss.market.maker.model.trade.design;

import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.ApplicationUtils;

import java.util.Map;

public enum TradeDesignType {
    FOLLOW {
        @Override
        public TradeDesignPolicy createTradeDesignPolicy(SymbolInfo symbolInfo) {
            return ApplicationUtils.getSpringBean(FollowTradeDesignPolicy.class,symbolInfo);
        }
    },
    TRIANGLE {
        @Override
        public TradeDesignPolicy createTradeDesignPolicy(SymbolInfo symbolInfo) {
            return ApplicationUtils.getSpringBean(TriangleTradeDesignPolicy.class,symbolInfo);
        }
    };

    public static TradeDesignPolicy createDefaultTradeDesignPolicy(SymbolInfo symbolInfo) {
        if(symbolInfo.getChildSymbol().size() == 1) {
            return FOLLOW.createTradeDesignPolicy(symbolInfo);
        } else if(symbolInfo.getChildSymbol().size() == 2) {
            return TRIANGLE.createTradeDesignPolicy(symbolInfo);
        } else  {
            throw new UnsupportedOperationException();
        }
    }

    public abstract TradeDesignPolicy createTradeDesignPolicy(SymbolInfo symbolInfo);

    private static Map<String, TradeDesignType> designTypeMap = new CacheMap<>(k->{
        TradeDesignType tradeDesignType = TradeDesignType.valueOf(k.toUpperCase());
        return tradeDesignType;
    });

    public static TradeDesignType getByName(String name) {
        return designTypeMap.get(name);
    }
}
