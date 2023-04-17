package com.wsss.market.maker.model.depth.design;
import com.wsss.market.maker.model.center.BootStrap;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;

import java.util.Map;

public enum DesignType {
    FOLLOW {
        @Override
        public MakerDesignPolicy createMakerDesignPolicy(SymbolInfo symbolInfo) {
            return BootStrap.getSpringBean(FollowMakerDesignPolicy.class,symbolInfo);
        }
    },
    TRIANGLE {
        @Override
        public MakerDesignPolicy createMakerDesignPolicy(SymbolInfo symbolInfo) {
            return null;
        }
    };

    public abstract MakerDesignPolicy createMakerDesignPolicy(SymbolInfo symbolInfo);

    private static Map<String, DesignType> designTypeMap = new CacheMap<>(k->{
        DesignType designType = DesignType.valueOf(k.toUpperCase());
        return designType;
    });

    public static DesignType getByName(String name) {
        return designTypeMap.get(name);
    }
}
