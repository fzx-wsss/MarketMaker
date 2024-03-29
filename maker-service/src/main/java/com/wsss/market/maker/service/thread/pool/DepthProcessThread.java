package com.wsss.market.maker.service.thread.pool;

import com.superatomfin.framework.monitor.Monitor;
import com.superatomfin.share.tools.other.TimeSieve;
import com.wsss.market.maker.inner.api.receive.DepthListenTask;
import com.wsss.market.maker.model.config.MakerConfig;
import com.wsss.market.maker.model.config.SymbolConfig;
import com.wsss.market.maker.model.depth.design.DepthDesignPolicy;
import com.wsss.market.maker.model.depth.design.MakerContext;
import com.wsss.market.maker.model.domain.CacheMap;
import com.wsss.market.maker.model.domain.Side;
import com.wsss.market.maker.model.domain.SubscribedOrderBook;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.model.limit.MakerLimitPolicy;
import com.wsss.market.maker.model.utils.BigDecimalUtils;
import com.wsss.market.maker.model.utils.Perf;
import com.wsss.market.maker.service.center.DataCenter;
import com.wsss.market.maker.service.task.AbstractAsyncTask;
import com.wsss.market.maker.service.task.CompositeTask;
import com.wsss.market.maker.service.task.MakeOrderTask;
import com.wsss.market.maker.service.task.QueryOwnerOrderTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Scope("prototype")
public class DepthProcessThread implements Runnable {
    private BlockingQueue<String> queue = new LinkedBlockingQueue(10000);
    @Resource
    private DataCenter dataCenter;
    @Resource
    private MarkerMakerThreadPool markerMakerThreadPool;
    @Resource
    private MakerConfig makerConfig;
    @Resource
    private SymbolConfig symbolConfig;

    private Map<String, AbstractAsyncTask> taskMap = new ConcurrentHashMap<>();
    private Map<String, TimeSieve> queryOwnerLimitMap = new CacheMap<>(k -> TimeSieve.builder().build());
    private Map<String, TimeSieve> makerLimitMap = new CacheMap<>(k -> TimeSieve.builder().build());

    @Override
    public void run() {
        Monitor.TimeContext timeContext = null;
        Monitor.CountContext count = Monitor.counter(Thread.currentThread().getName());
        while (true) {
            try {
                String symbol = queue.take();
                count.end();
                timeContext = Monitor.timer("depth_process_thread");
                SymbolInfo symbolInfo = dataCenter.getSymbolInfo(symbol);
                transferOrderBook(symbolInfo);

                CompositeTask task = CompositeTask.builder().symbolInfo(symbolInfo).build();
                queryOwnerLimitMap.get(symbol).moreThanExec(() -> {
                    task.addTask(QueryOwnerOrderTask.builder().symbol(symbolInfo).build());
                }, TimeUnit.SECONDS.toMillis(makerConfig.getSyncTime()));

                if (limitOrderFrequency(symbolInfo)) {
                    MakeOrderTask orderTask = createMakeOrderTask(symbolInfo);
                    if (orderTask != null) {
                        task.addTask(orderTask);
                        logOrderFrequency(symbolInfo, task);
                    }
                }

                if (task.isEmpty()) {
                    continue;
                }

                Perf.count("exec_depth_task", symbolInfo);
                // 下单和撤单
                markerMakerThreadPool.execAsyncTask(task);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                timeContext.end();
            }
        }
    }

    public boolean offer(String t) {
        return queue.offer(t);
    }


    private void logOrderFrequency(SymbolInfo symbolInfo, CompositeTask task) {
        taskMap.put(symbolInfo.getSymbol(), task);
        makerLimitMap.get(symbolInfo.getSymbol()).updateTime();
    }

    private boolean limitOrderFrequency(SymbolInfo symbolInfo) {
        // 是否重新摆盘订单
        MakerLimitPolicy limitPolicy = symbolInfo.getLimitPolicy();
        if (!limitPolicy.isOn()) {
            return false;
        }
        AbstractAsyncTask last = taskMap.get(symbolInfo.getSymbol());
        if (last != null && !last.isFinish()) {
            return false;
        }
        TimeSieve timeSieve = makerLimitMap.get(symbolInfo.getSymbol());
        if (timeSieve.lassThanThreshold(makerConfig.getMinMakeOrderTime())) {
            return false;
        }
        return true;
    }

    private MakeOrderTask createMakeOrderTask(SymbolInfo symbolInfo) {
        DepthDesignPolicy designPolicy = symbolInfo.getDepthDesignPolicy();

        // 计算需要的下单和撤单
        MakerContext context = designPolicy.designOrder();
        if (context == null
                || (CollectionUtils.isEmpty(context.getRemoveOrders())
                && CollectionUtils.isEmpty(context.getPlaceOrders()))) {
            return null;
        }
        MakeOrderTask orderTask = MakeOrderTask.builder()
                .symbolInfo(symbolInfo)
                .placeOrderList(context.getPlaceOrders())
                .cancelOrderList(context.getRemoveOrders())
                .build();
        return orderTask;
    }

    private void transferOrderBook(SymbolInfo symbolInfo) {
        Map<String, BlockingQueue> subscribedQueue = symbolInfo.getSubscribedQueueMap();
        subscribedQueue.forEach((k, v) -> {
            DepthListenTask task = null;
            while ((task = (DepthListenTask) v.poll()) != null) {
                // 同步订单簿
                task.transferOrderBook();
            }
        });
        for (SubscribedOrderBook orderBook : symbolInfo.getSubscribedOrderBookMap().values()) {
            checkOrderBook(orderBook, symbolInfo);
        }
    }

    private void checkOrderBook(SubscribedOrderBook subscribedOrderBook, SymbolInfo symbolInfo) {
        BigDecimal bestBuy = subscribedOrderBook.getBestBuy();
        BigDecimal bestSell = subscribedOrderBook.getBestSell();
        if (Objects.nonNull(bestBuy) && Objects.nonNull(bestSell) && bestBuy.compareTo(bestSell) >= 0) {
            long diff = bestBuy.subtract(bestSell).divide(bestSell,4,BigDecimal.ROUND_HALF_UP).multiply(BigDecimalUtils.WAN).longValue();
            if(diff > symbolConfig.getPriceDiff(symbolInfo.getSymbolAo())) {
                log.warn("{} price diff:{} bestBuy:{},{}; bestSell:{},{}",symbolInfo.getSymbol(),diff,
                        bestBuy,subscribedOrderBook.getSourceByPrice(bestBuy,Side.BUY),
                        bestSell,subscribedOrderBook.getSourceByPrice(bestSell,Side.SELL)
                );
            }
            Monitor.timer("source_price_diff").end(diff);
//            subscribedOrderBook.getNearerBooks(bestBuy, Side.SELL).forEach(d -> subscribedOrderBook.remove(Side.SELL, d.getPrice()));
//            subscribedOrderBook.getNearerBooks(bestSell, Side.BUY).forEach(d -> subscribedOrderBook.remove(Side.BUY, d.getPrice()));
        }
    }
}
