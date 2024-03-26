package com.wsss.market.maker.inner.supplier.okex;

import com.wsss.market.maker.inner.api.receive.DepthListenTask;
import com.wsss.market.maker.model.domain.*;
import com.wsss.market.maker.model.utils.Crc32Utils;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.math.BigDecimal;
import java.util.List;

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

    private boolean checkCrc32(int checksum) {
        List<Depth> buys = subscribedOrderBook.getNearerBooksBySource(Source.Okex,25, Side.BUY);
        List<Depth> sells = subscribedOrderBook.getNearerBooksBySource(Source.Okex,25,Side.SELL);
        StringBuilder sb = new StringBuilder();
        int i =0;
        for(;i<buys.size() && i < sells.size();i++) {
            Depth buy = buys.get(i);
            Depth sell = sells.get(i);
            sb.append(buy.getPrice().toPlainString() + ":" + buy.getVolume().toPlainString());
            sb.append(":");
            sb.append(sell.getPrice().toPlainString() + ":" + sell.getVolume().toPlainString());
        }
        for(;i<buys.size();i++) {
            Depth buy = buys.get(i);
            sb.append(":").append(buy.getPrice().toPlainString() + ":" + buy.getVolume().toPlainString());
        }
        for(;i < sells.size();i++) {
            Depth sell = sells.get(i);
            sb.append(":").append(sell.getPrice().toPlainString() + ":" + sell.getVolume().toPlainString());
        }
        int crc32 = Crc32Utils.crc32(sb.toString().getBytes());
        return crc32 != checksum;
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
