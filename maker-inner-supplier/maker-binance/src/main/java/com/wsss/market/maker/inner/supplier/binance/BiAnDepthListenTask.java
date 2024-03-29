package com.wsss.market.maker.inner.supplier.binance;

import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.inner.api.receive.DepthListenTask;
import com.wsss.market.maker.model.config.SourceConfig;
import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.Source;
import com.wsss.market.maker.model.domain.SubscribedOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.utils.HttpUtils;
import com.wsss.market.maker.model.utils.StringUtils;
import com.wsss.market.maker.utils.JacksonMapper;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BiAnDepthListenTask implements DepthListenTask {
    private SymbolInfo symbolInfo;
    private JsonNode json;
    private SubscribedOrderBook subscribedOrderBook;

    public BiAnDepthListenTask(SymbolInfo symbolInfo, JsonNode json, SubscribedOrderBook subscribedOrderBook) {
        this.symbolInfo = symbolInfo;
        this.json = json;
        this.subscribedOrderBook = subscribedOrderBook;
    }

    @Override
    public void transferOrderBook() {
        try {
            String evtName = json.get("e").asText();
            String symbol = StringUtils.toLowerSymbol(json.get("s").asText());
            long evtFirstId = json.get("U").asLong();
            long evtLastId = json.get("u").asLong();
//            log.info("{} receive msg:{}",symbol,json);
            if (!evtName.equals("depthUpdate")) {
                return;
            }

            long eventId = subscribedOrderBook.getEventId(Source.Binance);
            if(evtFirstId > eventId + 1 || eventId == 0) {
                log.info("{} init Bian evtFirstId:{},evtLastId:{},orderBook eventId:{}",symbol,evtFirstId,evtLastId, eventId);
                initOrderBook(symbol, subscribedOrderBook);
            }
            if(evtLastId <= eventId) {
                return;
            }

            if(evtFirstId != eventId + 1) {
                log.info("{} Bian evtFirstId:{},evtLastId:{},orderBook eventId:{}",symbol,evtFirstId,evtLastId, eventId);
            }
            JsonNode bids = json.get("b");
            JsonNode asks = json.get("a");
            updateOrderBook(evtLastId,bids,asks, subscribedOrderBook);
        } catch (Exception e) {
            log.error("error",e);
        }
    }

    private void initOrderBook(String symbol, SubscribedOrderBook subscribedOrderBook) throws IOException {
        String snap = requestHttpDepth(symbol);
        JsonNode root = JacksonMapper.getInstance().readTree(snap);
        JsonNode bids = root.get("bids");
        JsonNode asks = root.get("asks");
        long lastId = root.get("lastUpdateId").asLong();
        subscribedOrderBook.clear(Source.Binance);
        updateOrderBook(lastId,bids,asks, subscribedOrderBook);
    }

    private String requestHttpDepth(String symbol) throws IOException {
        Monitor.TimeContext context = Monitor.timer("bian_req_http_depth");
        try {
            //初始化
            String url = SourceConfig.getInstance().getBinanceDepthUrl();
            Map<String,String> map = new HashMap<>();
            map.put("symbol",symbol.toUpperCase());
            map.put("limit",SourceConfig.getInstance().getLimit());
            String snap = HttpUtils.doGetJson(url,null,map);
            return snap;
        } finally {
            context.end();
        }
    }

    private void updateOrderBook(long eventId, JsonNode buys,JsonNode sells, SubscribedOrderBook subscribedOrderBook) {
        updateOrderBook(Side.BUY,buys, subscribedOrderBook);
        updateOrderBook(Side.SELL,sells, subscribedOrderBook);
        subscribedOrderBook.setEventId(Source.Binance,eventId);
    }

    private void updateOrderBook(Side side, JsonNode priceLevels, SubscribedOrderBook subscribedOrderBook) {
        for (JsonNode pair : priceLevels) {
            BigDecimal price = new BigDecimal(pair.get(0).asText());
            BigDecimal volume = new BigDecimal(pair.get(1).asText());
            if(!subscribedOrderBook.update(side, price, volume, Source.Binance)) {
                // 当价格超出最大档位时跳出循环，不在进行后续更新
                // 可以这么操作的前提是，订单簿的档位是有序的
                break;
            }
        }
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
