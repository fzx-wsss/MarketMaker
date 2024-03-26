package com.wsss.market.maker.service.thread.pool;

import com.superatomfin.framework.monitor.Monitor;
import com.superatomfin.share.tools.other.Sleep;
import com.wsss.market.maker.inner.api.receive.TradeListenTask;
import com.wsss.market.maker.model.config.TradeConfig;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.domain.Trade;
import com.wsss.market.maker.model.limit.SlidingTimeMakerLimitPolicy;
import com.wsss.market.maker.model.utils.Perf;
import com.wsss.market.maker.service.center.DataCenter;
import com.wsss.market.maker.service.task.MakeTradeTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    private static CacheMap<String, SlidingTimeMakerLimitPolicy> limiterCacheMap = new CacheMap<>(k->new SlidingTimeMakerLimitPolicy());

    @Override
    public void run() {
        Monitor.TimeContext timeContext = null;
        while (true) {
            try {
                TradeListenTask tradeTask = queue.take();
                timeContext = Monitor.timer("trade_process_thread");

                Map<String,List<Trade>> map = convertTrade(tradeTask);
                execAsyncTask(map);

                if(tradeConfig.getSleep() > 0) {
                    Sleep.sleepMillisSeconds(tradeConfig.getSleep());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                timeContext.end();
            }
        }
    }

    public Map<String,List<Trade>> convertTrade(TradeListenTask tradeTask) {
        CacheMap<String,List<Trade>> map = new CacheMap<>(k->new ArrayList<>());

        while(tradeTask != null) {
            Monitor.counter(Thread.currentThread().getName()).end();
            SymbolInfo symbolInfo = tradeTask.getSymbol();

            if(!limiterCacheMap.get(symbolInfo.getSymbol()).isOn()) {
                Perf.count("disable_trade_msg",symbolInfo);
                tradeTask = queue.poll();
                continue;
            }

            Trade trade = tradeTask.logTrade();
            if(trade != null) {
                trade = tradeTask.getSymbol().getTradeDesignPolicy().designTrade(trade);
            }
            if(trade != null) {
                map.get(trade.getSymbol()).add(trade);
            }
            tradeTask = queue.poll();
        }
        return map;
    }

    private void execAsyncTask(Map<String,List<Trade>> map) {
        if(map.isEmpty()) {
            return;
        }

        map.entrySet().forEach(e -> {
            MakeTradeTask makeTradeTask = MakeTradeTask.builder()
                    .trades(e.getValue())
                    .symbolInfo(dataCenter.getSymbolInfo(e.getKey()))
                    .build();
            markerMakerThreadPool.execAsyncTask(makeTradeTask);
        });
    }

    public boolean offer(TradeListenTask t) {
        return queue.offer(t);
    }
}
