package com.wsss.market.maker.model.depth.subscribe.bian;

import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.model.depth.subscribe.TradeListenTask;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.TradeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.math.BigDecimal;

@Slf4j
public class BiAnTradeListenTask implements TradeListenTask {
    private SymbolInfo symbolInfo;
    private JsonNode json;
    private static CacheMap<String, TradeLimiter> limiterCacheMap = new CacheMap<>(k->new TradeLimiter());

    public BiAnTradeListenTask(SymbolInfo symbolInfo, JsonNode json) {
        this.symbolInfo = symbolInfo;
        this.json = json;
    }

    @Override
    public void logTrade() {
        try {
            if(limiterCacheMap.get(symbolInfo.getSymbol()).isLimit()) {
//                log.info("disable trade");
                return;
            }
            Monitor.counter("bi_an_real_trade_msg").end();
            if(symbolInfo.isDebugLog()) {
                log.info("trade:{}",json);
            }
        } catch (Exception e) {
            log.error("error",e);
        }
    }

    @Override
    public String getSymbol() {
        return symbolInfo.getSymbol();
    }

    public static void main(String[] args) {
        System.out.println(Side.BUY.isAfter(BigDecimal.TEN,BigDecimal.ONE));
        System.out.println(Side.SELL.isAfter(BigDecimal.ONE,BigDecimal.TEN));
    }

}
