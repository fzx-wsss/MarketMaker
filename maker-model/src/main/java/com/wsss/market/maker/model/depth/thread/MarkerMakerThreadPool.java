package com.wsss.market.maker.model.depth.thread;

import com.wsss.market.maker.model.center.BootStrap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@DependsOn("bootStrap")
public class MarkerMakerThreadPool {
    // 用于深度计算的线程
    private DepthProcessThread[] depthProcessThreads = new DepthProcessThread[Runtime.getRuntime().availableProcessors()];
    // 用于成交计算的线程
    private TradeProcessThread[] tradeProcessThreads = new TradeProcessThread[Runtime.getRuntime().availableProcessors()];
    // 用于io等待的线程
    private ExecutorService designOrderExecutor = new ThreadPoolExecutor(0, 500,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new ThreadFactory() {
                private AtomicInteger count = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setName("designTask-" + count.getAndIncrement());
                    return thread;
                }
            },
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    DesignOrderTask orderTask = (DesignOrderTask)r;
                    orderTask.finish();
                    String symbol = orderTask.getSymbolInfo().getSymbol();
                    log.error("executorService is full,symbol:{}",symbol);
                }
            }
    );

    @PostConstruct
    public void init() {
        for (int i = 0; i< depthProcessThreads.length; i++) {
            depthProcessThreads[i] = BootStrap.getSpringBean(DepthProcessThread.class);
            Thread thread = new Thread(depthProcessThreads[i]);
            thread.setDaemon(true);
            thread.setName(String.format("DepthProcessThread-%s",i));
            thread.start();
        }

        for (int i = 0; i< tradeProcessThreads.length; i++) {
            tradeProcessThreads[i] = BootStrap.getSpringBean(TradeProcessThread.class);
            Thread thread = new Thread(tradeProcessThreads[i]);
            thread.setDaemon(true);
            thread.setName(String.format("TradeProcessThread-%s",i));
            thread.start();
        }
    }

    public DepthProcessThread getDepthProcessThread(String symbol) {
        return depthProcessThreads[Math.abs(symbol.hashCode()) % depthProcessThreads.length];
    }
    public TradeProcessThread getTradeProcessThread(String symbol) {
        return tradeProcessThreads[Math.abs(symbol.hashCode()) % tradeProcessThreads.length];
    }

    public void execDesignOrderTask(DesignOrderTask task) {
        designOrderExecutor.execute(task);
    }

}
