package com.aliyun.iotx.redissto.support;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 时间和容量 滑动窗口
 *
 * @author jiehong.jh
 * @date 2018/4/27
 */
public class CacheSlideWindow<T> extends CountSlideWindow<T> {

    private final long time;
    private final TimeUnit unit;
    private final long window;

    private ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1,
        new SimpleThreadFactory("cache-slide-window"));

    /**
     * @param length bucket个数
     * @param head
     * @param max    每个bucket容量
     * @param time   每个bucket存活时间
     * @param unit
     */
    public CacheSlideWindow(int length, AbstractBucket<T> head, int max, long time, TimeUnit unit) {
        super(length, head, max);
        this.time = time;
        this.unit = unit;
        window = unit.toMillis(time);
        executor.schedule(this::keepNext, time, unit);
    }

    private void keepNext() {
        wLock.lock();
        try {
            if (System.currentTimeMillis() >= window + liveTime) {
                count.set(0);
                safeKeepNext();
            }
        } finally {
            wLock.unlock();
        }
    }

    @Override
    protected void onSafeNext() {
        executor.schedule(this::keepNext, time, unit);
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
