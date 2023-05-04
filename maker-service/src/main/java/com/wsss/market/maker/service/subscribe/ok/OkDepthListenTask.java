package com.wsss.market.maker.service.subscribe.ok;

import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.Source;
import com.wsss.market.maker.model.domain.SubscribedOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.service.subscribe.DepthListenTask;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.math.BigDecimal;

@Slf4j
public class OkDepthListenTask implements DepthListenTask {
    private SymbolInfo symbolInfo;
    private String childSymbol;
    private JsonNode json;
    private SubscribedOrderBook subscribedOrderBook;

    public OkDepthListenTask(SymbolInfo symbolInfo, JsonNode json, String childSymbol) {
        this.symbolInfo = symbolInfo;
        this.childSymbol = childSymbol;
        this.json = json;
        this.subscribedOrderBook = symbolInfo.getChildSubscribedOrderBook(childSymbol);
    }

    @Override
    public SymbolInfo getSymbol() {
        return symbolInfo;
    }

    @Override
    public void transferOrderBook() {
        String channel = json.get("arg").get("channel").asText();
        if (!channel.equals("books") || json.get("action") == null) {
            log.warn("unknown msg:{}",json);
            return;
        }
        String action = json.get("action").asText();
        if("snapshot".equals(action)) {
            subscribedOrderBook.clear(Source.Okex);
        }
        updateOrderBook();
    }

    private void updateOrderBook() {
        //初始化
        JsonNode bids = json.get("data").get(0).get("bids");
        JsonNode asks = json.get("data").get(0).get("asks");
        long lastId = json.get("data").get(0).get("ts").asLong();
        int checksum = json.get("data").get(0).get("checksum").asInt();

        updateOrderBook(lastId,bids,asks, subscribedOrderBook);
    }

    private void updateOrderBook(long eventId, JsonNode buys,JsonNode sells, SubscribedOrderBook subscribedOrderBook) {
        updateOrderBook(Side.BUY,buys, subscribedOrderBook);
        updateOrderBook(Side.SELL,sells, subscribedOrderBook);
        subscribedOrderBook.setEventId(Source.Okex,eventId);
    }

    private void updateOrderBook(Side side, JsonNode priceLevels, SubscribedOrderBook subscribedOrderBook) {
        for (JsonNode pair : priceLevels) {
            BigDecimal price = new BigDecimal(pair.get(0).asText());
            BigDecimal volume = new BigDecimal(pair.get(1).asText());
            if(!subscribedOrderBook.update(side, price, volume, Source.Okex)) {
                // 当价格超出最大档位时跳出循环，不在进行后续更新
                // 可以这么操作的前提是，订单簿的档位是有序的
                break;
            }
        }
    }
}
