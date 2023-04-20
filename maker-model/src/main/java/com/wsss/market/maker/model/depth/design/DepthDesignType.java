package com.wsss.market.maker.model.depth.design;

import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.ApplicationUtils;

import java.util.Map;

public enum DepthDesignType {
    FOLLOW {
        @Override
        public DepthDesignPolicy createMakerDesignPolicy(SymbolInfo symbolInfo) {
            return ApplicationUtils.getSpringBean(FollowDepthDesignPolicy.class, symbolInfo);
        }
    },
    TRIANGLE {
        @Override
        public DepthDesignPolicy createMakerDesignPolicy(SymbolInfo symbolInfo) {
            return ApplicationUtils.getSpringBean(TriangleDepthDesignPolicy.class, symbolInfo);
        }
    };

    public static DepthDesignPolicy createDefaultMakerDesignPolicy(SymbolInfo symbolInfo) {
        if (symbolInfo.getChildSymbol().size() == 1) {
            return FOLLOW.createMakerDesignPolicy(symbolInfo);
        } else if (symbolInfo.getChildSymbol().size() == 2) {
            return TRIANGLE.createMakerDesignPolicy(symbolInfo);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public abstract DepthDesignPolicy createMakerDesignPolicy(SymbolInfo symbolInfo);

    private static Map<String, DepthDesignType> designTypeMap = new CacheMap<>(k -> {
        DepthDesignType depthDesignType = DepthDesignType.valueOf(k.toUpperCase());
        return depthDesignType;
    });

    public static DepthDesignType getByName(String name) {
        return designTypeMap.get(name);
    }
}
