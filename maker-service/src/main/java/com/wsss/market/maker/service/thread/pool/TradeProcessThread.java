package com.wsss.market.maker.service.thread.pool;

import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.model.config.TradeConfig;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.Trade;
import com.wsss.market.maker.model.domain.TradeLimiter;
import com.wsss.market.maker.model.utils.Perf;
import com.wsss.market.maker.service.center.DataCenter;
import com.wsss.market.maker.service.subscribe.TradeListenTask;
import com.wsss.market.maker.service.task.MakeTradeTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Scope("prototype")
public class TradeProcessThread implements Runnable {
    private BlockingQueue<TradeListenTask> queue = new LinkedBlockingQueue(10000);
    @Resource
    private DataCenter dataCenter;
    @Resource
    private MarkerMakerThreadPool markerMakerThreadPool;
    @Resource
    private TradeConfig tradeConfig;
    private static CacheMap<String, TradeLimiter> limiterCacheMap = new CacheMap<>(k->new TradeLimiter());

    @Override
    public void run() {
        Monitor.TimeContext timeContext = null;
        Monitor.CountContext count = Monitor.counter(Thread.currentThread().getName());
        while (true) {
            try {
                TradeListenTask tradeTask = queue.take();
                timeContext = Monitor.timer("trade_process_thread");
                CacheMap<String,List<Trade>> map = new CacheMap<>(k->new ArrayList<>());
                while(tradeTask != null) {
                    Trade trade = convertTrade(tradeTask,count);
                    if(trade != null) {
                        map.get(trade.getSymbol()).add(trade);
                    }
                    tradeTask = queue.poll();
                }
                if(map.isEmpty()) {
                    continue;
                }

                map.entrySet().forEach(e -> {
                    MakeTradeTask makeTradeTask = MakeTradeTask.builder()
                            .trades(e.getValue())
                            .symbolInfo(dataCenter.getSymbolInfo(e.getKey()))
                            .build();
                    markerMakerThreadPool.execAsyncTask(makeTradeTask);
                });

                if(tradeConfig.getSleep() > 0) {
                    TimeUnit.MILLISECONDS.sleep(tradeConfig.getSleep());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                timeContext.end();
            }
        }
    }

    private Trade convertTrade(TradeListenTask tradeTask,Monitor.CountContext count) {
        SymbolInfo symbolInfo = tradeTask.getSymbol();
        if(limiterCacheMap.get(symbolInfo.getSymbol()).isLimit()) {
            Perf.count("disable_trade_msg",symbolInfo);
            return null;
        }
        count.end();

        Trade trade = tradeTask.logTrade();
        return trade;
    }

    public boolean offer(TradeListenTask t) {
        return queue.offer(t);
    }
}
