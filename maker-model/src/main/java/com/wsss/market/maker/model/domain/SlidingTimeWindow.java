package com.wsss.market.maker.model.domain;

import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class SlidingTimeWindow {
    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicReferenceArray<TimeSplit> arr;
    private final int interval;

    public SlidingTimeWindow(int interval) {
        this.interval = interval;
        arr = new AtomicReferenceArray<>(interval);
    }

    public int get() {
        return count.get();
    }

    public int getAndIncrement() {
        long time = System.currentTimeMillis() / 1000;
        int index = (int)(time % interval);
        TimeSplit ts = arr.get(index);
        while (ts == null || ts.getTime() != time) {
            TimeSplit newTs = new TimeSplit(time,new AtomicInteger(0));
            if(arr.compareAndSet(index,ts,newTs) && ts != null) {
                count.getAndAdd(-ts.getCount().get());
            }
            time = System.currentTimeMillis() / 1000;
            index = (int)(time % interval);
            ts = arr.get(index);
        }
        ts.getCount().getAndIncrement();
        return count.getAndIncrement();
    }

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
         int INTERVAL_SECONDS = 2;
        SlidingTimeWindow stw = new SlidingTimeWindow(INTERVAL_SECONDS);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        long start = System.currentTimeMillis();
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
        for(int i=0;i<stw.arr.length();i++) {
            totalCount += stw.arr.get(i).getCount().get();
        }
        int expectedCount = stw.get();

        System.out.println("Total request count: " + totalCount);
        System.out.println("Expected request count: " + expectedCount);
        System.out.println("Difference: " + (expectedCount - totalCount));
    }
}
