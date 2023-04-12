package com.wsss.market.maker.depth.thread;

import com.wsss.market.maker.center.ConfigCenter;
import com.wsss.market.maker.center.DataCenter;
import com.wsss.market.maker.config.MakerConfig;
import com.wsss.market.maker.depth.subscribe.DepthListenTask;
import com.wsss.market.maker.domain.*;
import com.wsss.market.maker.depth.design.MakerDesignPolicy;
import com.wsss.market.maker.depth.limit.MakerLimitPolicy;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.PRIVATE_MEMBER;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
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
        while (true) {
            try {
                String symbol = queue.take();
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
