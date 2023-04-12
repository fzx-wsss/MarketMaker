package com.wsss.market.maker.domain;

import org.apache.commons.math3.util.FastMath;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TradeLimiter {
    private int max;
    private AtomicInteger count = new AtomicInteger(0);
    private AtomicLong lastRefresh = new AtomicLong(0);

    public boolean isLimit() {
        int current = count.getAndIncrement();
        if(ThreadLocalRandom.current().nextDouble(FastMath.log(max)) < FastMath.log(current)) {
            return false;
        }

        return true;
    }
}
