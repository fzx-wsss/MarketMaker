package com.wsss.market.maker.service.subscribe.bian;

import com.wsss.market.maker.model.config.TradeConfig;
import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.Trade;
import com.wsss.market.maker.model.utils.Perf;
import com.wsss.market.maker.service.subscribe.TradeListenTask;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

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
        BigDecimal volume = getFixedVolume(new BigDecimal(json.get("q").asText()));
        if(volume == null) {
            return null;
        }
        BigDecimal price = getFixedPrice(new BigDecimal(json.get("p").asText()));
        boolean isBuy = json.get("m").asBoolean();

        return Trade.builder().symbol(symbolInfo.getSymbol()).askUserId(symbolInfo.getSymbolAo().getOffsetSellRobotId().intValue()).askId(0L)
                .bidUserId(symbolInfo.getSymbolAo().getOffsetBuyRobotId().intValue()).bidId(0L)
                .trendSide(isBuy ? Side.BUY : Side.SELL).price(price).volume(volume).ctime(new Date())
                .mtime(new Date()).build();
    }

    private BigDecimal getFixedPrice(BigDecimal originalPrice) {
        TradeConfig tradeConfig = TradeConfig.getInstance();
        BigDecimal strategy = tradeConfig.getPriceStrategy(symbolInfo.getSymbolAo());
        if(BigDecimal.ZERO.compareTo(strategy) == 0) {
            return originalPrice;
        }
        BigDecimal discount = ThreadLocalRandom.current().nextBoolean() ? BigDecimal.ONE.add(strategy) : BigDecimal.ONE.subtract(strategy);
        BigDecimal fixedPrice = originalPrice.multiply(discount).setScale(symbolInfo.getSymbolAo().getShowPriceScale(), BigDecimal.ROUND_DOWN);
        if(fixedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("{} fixed price less zero,strategy:{},",symbolInfo.getSymbol());
            return originalPrice;
        }
        return fixedPrice;
    }

    private BigDecimal getFixedVolume(BigDecimal originalVolume) {
        TradeConfig tradeConfig = TradeConfig.getInstance();
        BigDecimal discount = tradeConfig.getVolumeStrategy(symbolInfo.getSymbolAo());
        BigDecimal random = tradeConfig.getVolumeRandom(symbolInfo.getSymbolAo());
        random = ThreadLocalRandom.current().nextBoolean() ? BigDecimal.ONE.add(random) : BigDecimal.ONE.subtract(random);
        BigDecimal fixedVolume = originalVolume.multiply(discount).multiply(random).setScale(symbolInfo.getSymbolAo().getShowVolumeScale(), BigDecimal.ROUND_DOWN);
        if (fixedVolume.compareTo(symbolInfo.getSymbolAo().getTradeMinVolume()) < 0) {
            log.warn("{} volume too small,originalVolume:{},discount:{},random:{}",symbolInfo.getSymbol(),originalVolume,discount,random);
            return null;
        }
        return fixedVolume;
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
