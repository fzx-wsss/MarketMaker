package com.wsss.market.maker.service.thread.pool;

import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.model.config.MakerConfig;
import com.wsss.market.maker.model.depth.design.MakerContext;
import com.wsss.market.maker.model.depth.design.MakerDesignPolicy;
import com.wsss.market.maker.model.depth.limit.MakerLimitPolicy;
import com.wsss.market.maker.model.domain.SymbolInfo;
import com.wsss.market.maker.service.center.DataCenter;
import com.wsss.market.maker.service.subscribe.DepthListenTask;
import com.wsss.market.maker.service.task.DesignOrderTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

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

    private Map<String,DesignOrderTask> taskMap = new ConcurrentHashMap<>();

    @Override
    public void run() {
        Monitor.TimeContext timeContext = null;
        while (true) {
            try {
                String symbol = queue.take();
                timeContext = Monitor.timer("depth_process_thread");
                SymbolInfo symbolInfo = dataCenter.getSymbolInfo(symbol);
                transferOrderBook(symbolInfo);
                // 是否重新摆盘订单
                MakerLimitPolicy limitPolicy = symbolInfo.getLimitPolicy();
                if (!limitPolicy.isOn()) {
                    continue;
                }
                DesignOrderTask last = taskMap.get(symbol);
                if(!last.isFinish()) {
                    log.warn("last design order task not finish");
                    continue;
                }

                MakerDesignPolicy designPolicy = symbolInfo.getDesignPolicy();

                // 计算需要的下单和撤单
                MakerContext context =designPolicy.designOrder();
                if(context == null) {
                    log.warn("{} context is null", symbolInfo.getSymbol());
                    continue;
                }
                DesignOrderTask orderTask = DesignOrderTask.builder()
                        .placeOrderList(context.getPlaceOrders())
                        .cancelOrderList(context.getRemoveOrders())
                        .build();

                taskMap.put(symbolInfo.getSymbol(),orderTask);
                // 下单和撤单
                markerMakerThreadPool.execAsyncTask(orderTask);
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


    public void transferOrderBook(SymbolInfo symbolInfo) {
        Map<String,BlockingQueue> subscribedQueue = symbolInfo.getSubscribedQueueMap();
        subscribedQueue.forEach((k,v) -> {
            DepthListenTask task = null;
            while ((task = (DepthListenTask) v.poll()) != null) {
                // 同步订单簿
                task.transferOrderBook();
            }
        });
    }
}
