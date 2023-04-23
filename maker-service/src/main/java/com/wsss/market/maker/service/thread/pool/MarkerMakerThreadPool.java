package com.wsss.market.maker.service.thread.pool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wsss.market.maker.model.utils.ApplicationUtils;
import com.wsss.market.maker.service.task.AbstractAsyncTask;
import com.wsss.market.maker.service.task.AsyncTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@DependsOn("applicationUtils")
public class MarkerMakerThreadPool {
    // 用于深度计算的线程
    private DepthProcessThread[] depthProcessThreads = new DepthProcessThread[Runtime.getRuntime().availableProcessors()];
    // 用于成交计算的线程
    private TradeProcessThread[] tradeProcessThreads = new TradeProcessThread[Runtime.getRuntime().availableProcessors()];
    // 用于io等待的线程
    private ExecutorService designOrderExecutor = new ThreadPoolExecutor(100, 500,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(3000),
            new ThreadFactory() {
                private AtomicInteger count = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setName("invoke-task-" + count.getAndIncrement());
                    return thread;
                }
            },
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    if(r instanceof FutureTask) {
                        r.run();
                        log.error("executorService is full");
                        return;
                    }
                    AbstractAsyncTask orderTask = (AbstractAsyncTask)r;
                    orderTask.finish();
                    String symbol = orderTask.getSymbolInfo().getSymbol();
                    log.error("executorService is full,symbol:{}",symbol);
                }
            }
    );

    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("share-thread").build());

    @PostConstruct
    public void init() {
        for (int i = 0; i< depthProcessThreads.length; i++) {
            depthProcessThreads[i] = ApplicationUtils.getSpringBean(DepthProcessThread.class);
            Thread thread = new Thread(depthProcessThreads[i]);
            thread.setDaemon(true);
            thread.setName(String.format("DepthProcessThread-%s",i));
            thread.start();
        }

        for (int i = 0; i< tradeProcessThreads.length; i++) {
            tradeProcessThreads[i] = ApplicationUtils.getSpringBean(TradeProcessThread.class);
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

    public void execAsyncTask(Runnable task) {
        designOrderExecutor.execute(task);
    }

    public <T> Future<T> execAsyncTask(Callable<T> task) {
        return designOrderExecutor.submit(task);
    }

    public static ScheduledExecutorService getShareExecutor() {
        return executorService;
    }

    public static void main(String[] args) {
        MarkerMakerThreadPool pool = new MarkerMakerThreadPool();
        pool.execAsyncTask(new AsyncTask<Object>() {

            @Override
            public Object call() throws Exception {
                TimeUnit.MINUTES.sleep(1);
                return null;
            }
        });
        pool.execAsyncTask(new AsyncTask<Object>() {

            @Override
            public Object call() throws Exception {
                TimeUnit.MINUTES.sleep(1);
                return null;
            }
        });
        pool.execAsyncTask(new AsyncTask<Object>() {

            @Override
            public Object call() throws Exception {
                TimeUnit.MINUTES.sleep(1);
                return null;
            }
        });
    }
}
