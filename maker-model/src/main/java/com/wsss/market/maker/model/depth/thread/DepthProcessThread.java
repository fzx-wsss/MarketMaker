package com.wsss.market.maker.model.depth.thread;

import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.depth.subscribe.DepthListenTask;
import com.wsss.market.maker.model.center.DataCenter;
import com.wsss.market.maker.model.config.MakerConfig;
import com.wsss.market.maker.model.depth.design.MakerDesignPolicy;
import com.wsss.market.maker.model.depth.limit.MakerLimitPolicy;
import com.wsss.market.maker.model.domain.SymbolInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
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
                if(!symbolInfo.isAllDesignOrderTasksFinished()) {
                    log.warn("last design order task not finish");
                    continue;
                }

                MakerDesignPolicy designPolicy = symbolInfo.getDesignPolicy();

                // 计算需要的下单和撤单
                DesignOrderTask orderTask = designPolicy.designOrder();
                if(orderTask == null) {
                    log.warn("{} order task is null", symbolInfo.getSymbol());
                    continue;
                }
                symbolInfo.addDesignOrderTask(orderTask);
                // 下单和撤单
                markerMakerThreadPool.execDesignOrderTask(orderTask);
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
        Map<String,BlockingQueue<DepthListenTask>> subscribedQueue = symbolInfo.getSubscribedQueueMap();
        subscribedQueue.forEach((k,v) -> {
            DepthListenTask task = null;
            while ((task = v.poll()) != null) {
                // 同步订单簿
                task.transferOrderBook();
            }
        });
    }
}
