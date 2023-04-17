package com.wsss.market.maker.model.depth.thread;

import com.superatomfin.framework.monitor.Monitor;
import com.wsss.market.maker.model.center.DataCenter;
import com.wsss.market.maker.model.config.MakerConfig;
import com.wsss.market.maker.model.depth.subscribe.TradeListenTask;
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
        Monitor.TimeContext timeContext = null;
        while (true) {
            try {
                TradeListenTask tradeTask = queue.take();
                timeContext = Monitor.timer("trade_process_thread");
                tradeTask.logTrade();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                timeContext.end();
            }
        }
    }

    public boolean offer(TradeListenTask t) {
        return queue.offer(t);
    }
}
