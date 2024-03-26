package com.wsss.market.maker.inner.supplier.okex;

import com.wsss.market.maker.inner.api.receive.TradeListenTask;
import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.Trade;
import com.wsss.market.maker.model.utils.Perf;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
public class OkTradeListenTask implements TradeListenTask {
    private SymbolInfo symbolInfo;
    private JsonNode json;
    private String childSymbol;

    public OkTradeListenTask(SymbolInfo symbolInfo, JsonNode json, String childSymbol) {
        this.symbolInfo = symbolInfo;
        this.json = json;
        this.childSymbol = childSymbol;
    }

    @Override
    public SymbolInfo getSymbol() {
        return symbolInfo;
    }

    @Override
    public Trade logTrade() {
        Perf.count("ok_real_trade_msg",symbolInfo);
        if(symbolInfo.isDebugLog()) {
            log.info("ok trade:{}",json);
        }
        String channel = json.get("arg").get("channel").asText();
        if (!channel.equals("trades") || json.get("data") == null) {
            log.warn("unknown msg:{}",json);
            return null;
        }
        JsonNode root = json.get("data").get(0);
        BigDecimal volume = new BigDecimal(root.get("sz").asText());
        BigDecimal price = new BigDecimal(root.get("px").asText());
        boolean isBuy = "buy".equals(root.get("side").asText());
        return Trade.builder().symbol(childSymbol).askUserId(symbolInfo.getSymbolAo().getOffsetSellRobotId().intValue()).askId(0L)
                .bidUserId(symbolInfo.getSymbolAo().getOffsetBuyRobotId().intValue()).bidId(0L)
                .trendSide(isBuy ? Side.BUY : Side.SELL).price(price).volume(volume).ctime(new Date())
                .mtime(new Date()).build();
    }
}
