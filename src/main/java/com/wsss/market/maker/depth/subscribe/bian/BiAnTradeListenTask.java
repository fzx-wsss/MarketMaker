package com.wsss.market.maker.depth.subscribe.bian;

import com.wsss.market.maker.center.ConfigCenter;
import com.wsss.market.maker.depth.subscribe.DepthListenTask;
import com.wsss.market.maker.depth.subscribe.TradeListenTask;
import com.wsss.market.maker.domain.Side;
import com.wsss.market.maker.domain.Source;
import com.wsss.market.maker.domain.SubscribedOrderBook;
import com.wsss.market.maker.domain.SymbolInfo;
import com.wsss.market.maker.utils.HttpUtils;
import com.wsss.market.maker.utils.JacksonMapper;
import com.wsss.market.maker.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BiAnTradeListenTask implements TradeListenTask {
    private SymbolInfo symbolInfo;
    private JsonNode json;

    public BiAnTradeListenTask(SymbolInfo symbolInfo, JsonNode json) {
        this.symbolInfo = symbolInfo;
        this.json = json;
    }

    @Override
    public void logTrade() {
        try {
            log.info("trade:{}",json);
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