package com.wsss.market.maker.model.depth.limit;

import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;

import java.util.Map;

public enum LimitType {
    // 推送一次摆一次
    ALWAYS {
        @Override
        public MakerLimitPolicy createMakerLimitPolicy(SymbolInfo symbolInfo) {
            return new AlwaysMakerLimitPolicy();
        }
    },
    // 推送N次摆一次
    INTERVAL {
        @Override
        public MakerLimitPolicy createMakerLimitPolicy(SymbolInfo symbolInfo) {
            return null;
        }
    },
    // 按固定时间摆一次
    FIXED {
        @Override
        public MakerLimitPolicy createMakerLimitPolicy(SymbolInfo symbolInfo) {
            return null;
        }
    };

    public abstract MakerLimitPolicy createMakerLimitPolicy(SymbolInfo symbolInfo);

    private static Map<String,LimitType> limitTypeMap = new CacheMap<>(k->{
        LimitType limitType = LimitType.valueOf(k.toUpperCase());
        return limitType;
    });

    public static LimitType getByName(String name) {
        return limitTypeMap.get(name);
    }
}