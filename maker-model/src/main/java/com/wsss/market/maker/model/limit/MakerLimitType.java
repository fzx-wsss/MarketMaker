package com.wsss.market.maker.model.limit;

import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;

import java.util.Map;

public enum MakerLimitType {
    // 不限制
    ALWAYS {
        @Override
        public MakerLimitPolicy createMakerLimitPolicy(SymbolInfo symbolInfo) {
            return new AlwaysMakerLimitPolicy();
        }
    },
    // N次放过一次
    INTERVAL {
        @Override
        public MakerLimitPolicy createMakerLimitPolicy(SymbolInfo symbolInfo) {
            return null;
        }
    },
    // 按固定时间放过一次
    FIXED {
        @Override
        public MakerLimitPolicy createMakerLimitPolicy(SymbolInfo symbolInfo) {
            return null;
        }
    },
    SLIDING_TIME {
        @Override
        public MakerLimitPolicy createMakerLimitPolicy(SymbolInfo symbolInfo) {
            return new SlidingTimeMakerLimitPolicy();
        }
    };

    public abstract MakerLimitPolicy createMakerLimitPolicy(SymbolInfo symbolInfo);

    private static Map<String, MakerLimitType> limitTypeMap = new CacheMap<>(k->{
        MakerLimitType makerLimitType = MakerLimitType.valueOf(k.toUpperCase());
        return makerLimitType;
    });

    public static MakerLimitType getByName(String name) {
        return limitTypeMap.get(name);
    }
}
