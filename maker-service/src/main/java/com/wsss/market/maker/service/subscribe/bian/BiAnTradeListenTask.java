package com.wsss.market.maker.service.subscribe.bian;

import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.Trade;
import com.wsss.market.maker.model.utils.Perf;
import com.wsss.market.maker.service.subscribe.TradeListenTask;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
public class BiAnTradeListenTask implements TradeListenTask {
    private SymbolInfo symbolInfo;
    private JsonNode json;

    public BiAnTradeListenTask(SymbolInfo symbolInfo, JsonNode json) {
        this.symbolInfo = symbolInfo;
        this.json = json;
    }

    @Override
    public Trade logTrade() {
        Perf.count("bi_an_real_trade_msg",symbolInfo);
        if(symbolInfo.isDebugLog()) {
            log.info("trade:{}",json);
        }
        BigDecimal volume = new BigDecimal(json.get("q").asText());
        BigDecimal price = new BigDecimal(json.get("p").asText());
        boolean isBuy = json.get("m").asBoolean();

        return Trade.builder().symbol(symbolInfo.getSymbol()).askUserId(symbolInfo.getSymbolAo().getOffsetSellRobotId().intValue()).askId(0L)
                .bidUserId(symbolInfo.getSymbolAo().getOffsetBuyRobotId().intValue()).bidId(0L)
                .trendSide(isBuy ? Side.BUY : Side.SELL).price(price).volume(volume).ctime(new Date())
                .mtime(new Date()).build();
    }

    @Override
    public SymbolInfo getSymbol() {
        return symbolInfo;
    }

    public static void main(String[] args) {
        System.out.println(Side.BUY.isAfter(BigDecimal.TEN,BigDecimal.ONE));
        System.out.println(Side.SELL.isAfter(BigDecimal.ONE,BigDecimal.TEN));
    }

}
