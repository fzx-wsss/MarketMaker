package com.wsss.market.maker.model.utils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * 非线程安全的，多线程下可能会同时执行
 *
 */
public class TimeSieve {
    /**
     * 最后一次记录时间
     */
    private volatile long time;
    /**
     * 阈值
     */
    private long threshold;

    public void updateTime() {
        time = System.currentTimeMillis();
    }

    /**
     * 当前时间与上次时间的差值
     * @return
     */
    public long diff() {
        return System.currentTimeMillis() - time;
    }

    // 是否超出阈值
    public boolean moreThanThreshold() {
        return diff() > threshold;
    }
    public boolean moreThanThreshold(long threshold) {
        return diff() > threshold;
    }

    // 是否小于阈值
    public boolean lassThanThreshold() {
        return diff() < threshold;
    }
    public boolean lassThanThreshold(long threshold) {
        return diff() < threshold;
    }

    /**
     * {@linkplain TimeSieve#moreThanExec(Action, long)}
     * @param action
     * @return
     */
    public boolean moreThanExec(Action action) {
        if(threshold == 0) {
            throw new UnsupportedOperationException("threshold is 0");
        }
        return moreThanExec(action, threshold);
    }

    /**
     * 超出一定时间后才会执行
     * @param action
     * @param threshold
     * @return
     */
    public boolean moreThanExec(Action action,long threshold) {
        long current = System.currentTimeMillis();
        if(current - time > threshold) {
            action.exec();
            time = current;
            return true;
        }
        return false;
    }

    @FunctionalInterface
    public interface Action {
        void exec();
    }
}
