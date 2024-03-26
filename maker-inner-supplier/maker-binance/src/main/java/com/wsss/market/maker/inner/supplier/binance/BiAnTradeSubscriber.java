package com.wsss.market.maker.inner.supplier.binance;

import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.Perf;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@Scope("prototype")
public class BiAnTradeSubscriber extends BiAnAbstractSubscriber {
    private static String suffix = "@aggTrade";
    private static Map<String,String> symbolConvertMap = new CacheMap<>(s->s.substring(0,s.length()-suffix.length()));

    @Override
    protected void notifyProcessThread(SymbolInfo symbolInfo, String childSymbolName, JsonNode data) {
        Perf.count("bi_an_receive_trade_msg",symbolInfo);
        if(symbolInfo.isDebugLog()) {
            log.info("bi_an_receive_trade_msg:{}",data);
        }
        // 按主币对划分执行线程
        BiAnTradeListenTask task = new BiAnTradeListenTask(symbolInfo,data);
        markerMakerThreadPool.offerTrade(symbolInfo.getSymbol(), task);
    }

    @Override
    protected String getSuffix() {
        return suffix;
    }

    @Override
    protected String convertSymbolName(String name) {
        return symbolConvertMap.get(name);
    }
}
