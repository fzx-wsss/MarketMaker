package com.wsss.market.maker.service.subscribe.bybit;

import com.fasterxml.jackson.databind.JsonNode;
import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.Source;
import com.wsss.market.maker.model.domain.SubscribedOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.service.subscribe.DepthListenTask;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class BybitDepthListenTask implements DepthListenTask {
    private SymbolInfo symbolInfo;
    private String childSymbol;
    private JsonNode json;
    private SubscribedOrderBook subscribedOrderBook;

    public BybitDepthListenTask(SymbolInfo symbolInfo, JsonNode json, String childSymbol) {
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
        String action = json.get("type").asText();
        if("snapshot".equals(action)) {
            subscribedOrderBook.clear(Source.Bybit);
        }
        updateOrderBook();
    }

    private void updateOrderBook() {
        //初始化
        JsonNode bids = json.get("data").get("b");
        JsonNode asks = json.get("data").get("a");
        long lastId = json.get("ts").asLong();
        int seq = json.get("data").get("seq").asInt();
        long u = json.get("data").get("u").asInt();
        long eventId = subscribedOrderBook.getEventId(Source.Bybit);
        if(eventId != 0 && u != eventId + 1) {
            log.warn("u:{},eventId:{}",u,lastId);
        }
        updateOrderBook(u,bids,asks, subscribedOrderBook);
    }

    private void updateOrderBook(long eventId, JsonNode buys,JsonNode sells, SubscribedOrderBook subscribedOrderBook) {
        updateOrderBook(Side.BUY,buys, subscribedOrderBook);
        updateOrderBook(Side.SELL,sells, subscribedOrderBook);
        subscribedOrderBook.setEventId(Source.Bybit,eventId);
    }

    private void updateOrderBook(Side side, JsonNode priceLevels, SubscribedOrderBook subscribedOrderBook) {
        for (JsonNode pair : priceLevels) {
            BigDecimal price = new BigDecimal(pair.get(0).asText());
            BigDecimal volume = new BigDecimal(pair.get(1).asText());
            if(!subscribedOrderBook.update(side, price, volume, Source.Bybit)) {
                // 当价格超出最大档位时跳出循环，不在进行后续更新
                // 可以这么操作的前提是，订单簿的档位是有序的
                break;
            }
        }
    }
}
