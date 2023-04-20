package com.wsss.market.maker.model.config;

import com.cmcm.finance.ccc.client.model.SymbolAoWithFeatureAndExtra;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 摆盘配置参数
 */
@Slf4j
@Component
public class MakerConfig {
    private static MakerConfig cache;
    /**
     * 是否开启用户单避让和吃单，默认开启
     */
    @Getter
    @Value("${user.bulk.order.switch:true}")
    private boolean userBulkOrderSwitch;

    /**
     * 内存中保存的订阅的最大深度限制
     */
    @Getter
    @Value("${mem.order.book.limit:200}")
    private int memOrderBookLimit;

    /**
     * 让出盘口的百分比，整数值
     * 如果设置了让出盘口的档位值，则此配置不起作用
     * 假设配置为a,盘口买一为x，卖一为y
     * 则盘口卖单向上提高 (a/2/10000),摆盘卖单从y*(1+(a/2/10000))价位开始摆
     * 盘口买单向下降低 (a/2/10000)，摆盘买单从x*(1-(a/2/10000))价位开始摆
     */
    @Value("${min.spread.limit:1}")
    private int spreadLowLimitMillesimal;
    //lokiConfig
    private static String CFG_SPREAD_LOWLIMIT_MILLESIMAL = SymbolConfig.LOKI_CONFIG + "spreadLowLimitMillesimal";

    /**
     * 让出盘口的档位，整数值
     * 假设配置为a,盘口买一为x，卖一为y
     * 则盘口卖单向上提高 (a/2/10000),摆盘卖单从y*(1+(a/2/10000))价位开始摆
     * 盘口买单向下降低 (a/2/10000)，摆盘买单从x*(1-(a/2/10000))价位开始摆
     */
    //lokiConfig
    private static String BUY_GET_OUT_BPS = SymbolConfig.LOKI_CONFIG + "buyGetOutBps";
    private static String SELL_GET_OUT_BPS = SymbolConfig.LOKI_CONFIG + "sellGetOutBps";


    /**
     * 避让
     */
    @ApolloJsonValue("${user.bulk.order.limit:[\"7.5471\",\"400000\",\"800000\",\"250\",\"400000\"]}")
    private List<BigDecimal> userBulkOrderLimit;
    //lokiConfig
    String USER_BULK_ORDER_LIMIT = SymbolConfig.LOKI_CONFIG + "userBulkLimit";

    /**
     * 吃单
     */
    @Value("${user.bulk.order.strike.off.bps:30}")
    private int userBulkOrderStrikeOffBps;
    //lokiConfig
    String USER_BULK_ORDER_STRIKE = SymbolConfig.LOKI_CONFIG + "userBulkStrike";

    /**
     * 最大深度
     */
    @Value("${max.level:100}")
    private int maxLevel;
    //lokiConfig
    String CFG_MAX_LEVEL = SymbolConfig.LOKI_CONFIG + "maxLevel";

    /**
     * 深度volume的倍数和随机数
     */
    @Value("${depth.vol.random.num:0.1}")
    private double volRandomNum;
    String VOL_RANDOM_NUM = SymbolConfig.LOKI_CONFIG + "depthRandom";
    @Value("${depth.vol.multiple.num:1}")
    private double volMultipleNum;
    String VOL_MULTIPLE_NUM = SymbolConfig.LOKI_CONFIG + "depthMultiple";

    @Value("${depth.design.type.default:FOLLOW}")
    private String defaultDesign;
    @Value("${depth.limit.type.default:ALWAYS}")
    private String defaultLimit;
    @Getter
    @Value("${depth.limit.make.min.time:99}")
    private int minMakeOrderTime;

    @Getter
    @Value("${depth.sync.order.min.time:10}")
    private int minSyncTime;

    @Getter
    @Value("${depth.sync.order.time:30}")
    private int syncTime;

    @Getter
    @Value("${depth.make.order.random.replace.rate:1}")
    private int replaceRate;

    @Getter
    @Value("${depth.make.order.random.replace.interval:5}")
    private int replaceInterval;

    @Getter
    @Value("${depth.make.order.cancel.sleep:10}")
    private int cancelOrderSleep;




    public int getSpreadLowLimitMillesimal(SymbolAoWithFeatureAndExtra symbolInfo) {
        Integer res = SymbolConfig.getJsonNodeValue(symbolInfo,CFG_SPREAD_LOWLIMIT_MILLESIMAL,Integer.class);
        if(res != null) {
            return res;
        }
        return spreadLowLimitMillesimal;
    }

    public int getOneSideGetOutBps(SymbolAoWithFeatureAndExtra symbolInfo, Side side) {
        // 默认没有单边上调/下调价格档位。
        Integer res = SymbolConfig.getJsonNodeValue(symbolInfo,side == Side.BUY ? BUY_GET_OUT_BPS : SELL_GET_OUT_BPS,Integer.class);
        if(res != null) {
            return res;
        }
        return 0;
    }

    public BigDecimal getUserBulkOrderLimit(SymbolAoWithFeatureAndExtra symbolInfo) {
        BigDecimal res = SymbolConfig.getJsonNodeValue(symbolInfo,USER_BULK_ORDER_LIMIT,BigDecimal.class);
        if(res != null) {
            return res;
        }
        return getUserBulkOrderLimit(symbolInfo.getSymbolName());
    }

    public BigDecimal getUserBulkOrderLimit(String symbolName) {
        if (symbolName.endsWith("BTC")) {
            return userBulkOrderLimit.get(0);
        } else if (symbolName.endsWith("USDT") || symbolName.endsWith("USDC") || symbolName.endsWith("USD")) {
            return userBulkOrderLimit.get(1);
        } else if (symbolName.endsWith("XRP")) {
            return userBulkOrderLimit.get(2);
        } else if (symbolName.endsWith("ETH")) {
            return userBulkOrderLimit.get(3);
        }
        return null;
    }

    public int getUserBulkOrderStrikeOffBps(SymbolAoWithFeatureAndExtra symbolInfo) {
        Integer res = SymbolConfig.getJsonNodeValue(symbolInfo,USER_BULK_ORDER_STRIKE,Integer.class);
        if(res != null) {
            return res;
        }
        return userBulkOrderStrikeOffBps;
    }

    public int getMaxLevel(SymbolAoWithFeatureAndExtra symbolInfo) {
        Integer res = SymbolConfig.getJsonNodeValue(symbolInfo,CFG_MAX_LEVEL,Integer.class);
        if(res != null && res > 0) {
            return res;
        }
        return maxLevel;
    }

    public double getVolRandomNum(SymbolAoWithFeatureAndExtra symbolInfo) {
        Double res = SymbolConfig.getJsonNodeValue(symbolInfo,VOL_RANDOM_NUM,Double.class);
        if(res != null) {
            return res;
        }
        return volRandomNum;
    }

    public double getVolMultipleNum(SymbolAoWithFeatureAndExtra symbolInfo) {
        Double res = SymbolConfig.getJsonNodeValue(symbolInfo,VOL_MULTIPLE_NUM,Double.class);
        if(res != null) {
            return res;
        }
        return volMultipleNum;
    }

    public String getDesignType(SymbolAoWithFeatureAndExtra symbolInfo) {
        return defaultDesign;
    }

    public String getLimitType(SymbolAoWithFeatureAndExtra symbolInfo) {
        return defaultLimit;
    }

    public static MakerConfig getInstance() {
        if(cache == null) {
            cache = ApplicationUtils.getSpringBean(MakerConfig.class);
        }
        return cache;
    }
}
