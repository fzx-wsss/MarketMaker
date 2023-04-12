package com.wsss.market.maker.depth.thread;

import com.wsss.market.maker.center.DataCenter;
import com.wsss.market.maker.config.MakerConfig;
import com.wsss.market.maker.depth.design.MakerDesignPolicy;
import com.wsss.market.maker.depth.limit.MakerLimitPolicy;
import com.wsss.market.maker.depth.subscribe.DepthListenTask;
import com.wsss.market.maker.depth.subscribe.TradeListenTask;
import com.wsss.market.maker.domain.SymbolInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
    private MakerConfig makerConfig;

    @Override
    public void run() {
        while (true) {
            try {
                TradeListenTask tradeTask = queue.take();
                tradeTask.logTrade();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean offer(TradeListenTask t) {
        return queue.offer(t);
    }
}
