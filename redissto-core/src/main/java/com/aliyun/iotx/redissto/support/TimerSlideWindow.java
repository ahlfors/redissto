package com.aliyun.iotx.redissto.support;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 时间 滑动窗口
 *
 * @author jiehong.jh
 * @date 2018/4/27
 */
public class TimerSlideWindow<T> extends AbstractSlideWindow<T> {

    private ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1,
        new SimpleThreadFactory("timer-slide-window"));

    /**
     * @param length bucket个数
     * @param head
     * @param time   每个bucket存活时间
     * @param unit
     */
    public TimerSlideWindow(int length, AbstractBucket<T> head, long time, TimeUnit unit) {
        super(length, head);
        executor.scheduleAtFixedRate(this::safeKeepNext, time, time, unit);
    }

    @Override
    public void put(T t) {
        rLock.lock();
        arr[index].onAccept(t);
        rLock.unlock();
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
