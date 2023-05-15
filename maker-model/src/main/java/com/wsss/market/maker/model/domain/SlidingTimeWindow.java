package com.wsss.market.maker.model.domain;

import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 线程安全的滑动时间窗口计数，时间单位：秒
 */
public class SlidingTimeWindow {
    // 时间窗口内的总记录值
    private final AtomicInteger count = new AtomicInteger(0);
    // 每个时间点内的记录值
    private final AtomicReferenceArray<TimeSplit> arr;
    // 时间窗口的大小，单位秒
    private final int interval;
    // 最后一次处理的周期数，主要用于当长时间未被调用时更新数据使用
    private volatile long lastCycle;

    // 窗口时间长度
    public SlidingTimeWindow(int interval) {
        this.interval = interval;
        arr = new AtomicReferenceArray<>(interval);
        this.lastCycle = getCurrentCycle();
    }

    // 窗口时间内的记录值
    public int get() {
        long currentCycle = getCurrentCycle();
        if(currentCycle <= lastCycle) {
            return count.get();
        }
        updateTs(currentCycle);
        return count.get();
    }
    // 窗口时间内的记录值并加1
    public int getAndIncrement() {
        long currentCycle = getCurrentCycle();
        TimeSplit ts = updateTs(currentCycle);
        ts.getCount().getAndIncrement();
        return count.getAndIncrement();
    }

    // 计算所属时间周期
    private long getCurrentCycle() {
        return System.currentTimeMillis() / 1000;
    }

    // 更新时间窗口内的记录值
    private TimeSplit updateTs(long currentCycle) {
        long lastCycleTemp = Math.max(this.lastCycle, currentCycle - interval);
        if(currentCycle > lastCycleTemp) {
            // 更新
            this.lastCycle = currentCycle;
        } else if(currentCycle < lastCycleTemp) {
            // 避免机器发生时间回拨导致的错误
            lastCycleTemp = currentCycle;
        }

        TimeSplit ts = null;
        for(;lastCycleTemp<=currentCycle;lastCycleTemp++) {
            // 依次更新每个时间点的数据
            ts = doUpdateTs(lastCycleTemp);
        }
        return ts;
    }

    /**
     * 更新指定时间点的数据
     * 覆盖已经过期的数据，将过期数据从总记录值中减去
     * @param time
     * @return
     */
    private TimeSplit doUpdateTs(long time) {
        int index = (int)(time % interval);
        TimeSplit ts = arr.get(index);
        while (ts == null || ts.getTime() != time) {
            TimeSplit newTs = new TimeSplit(time,new AtomicInteger(0));
            if(arr.compareAndSet(index,ts,newTs) && ts != null) {
                count.getAndAdd(-ts.getCount().get());
            }
            ts = arr.get(index);
        }
        return ts;
    }

    /**
     * 记录每个时间点的值，
     * 当时间点过期时，用于移除总值中该时间点的记录值
     */
    @Getter
    private class TimeSplit {
        private final long time;
        private final AtomicInteger count;

        public TimeSplit(long time, AtomicInteger count) {
            this.time = time;
            this.count = count;
        }
    }

    public static void main(String[] args) throws InterruptedException {
         int THREAD_POOL_SIZE = 2;
         int TEST_TIME_SECONDS = 10;
         int INTERVAL_SECONDS = 5;
        SlidingTimeWindow stw = new SlidingTimeWindow(INTERVAL_SECONDS);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        long start = System.currentTimeMillis();
        System.out.println("start" + start);
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            executorService.execute(() -> {
                while (System.currentTimeMillis() - start <= TEST_TIME_SECONDS * 1000) {
                    stw.getAndIncrement();
                }
            });
        }

        while (System.currentTimeMillis() - start <= (TEST_TIME_SECONDS+1) * 1000) {
            Thread.sleep(1000);
            System.out.println("waiting");
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        int totalCount = 0;
        int expectedCount = stw.get();
        for(int i=0;i<stw.arr.length();i++) {
            totalCount += stw.arr.get(i).getCount().get();
        }


        System.out.println("Total request count: " + totalCount);
        System.out.println("Expected request count: " + expectedCount);
        System.out.println("Difference: " + (expectedCount - totalCount));
        for(int i=1;i<INTERVAL_SECONDS;i++) {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("current:" + stw.get());
        }
    }
}
