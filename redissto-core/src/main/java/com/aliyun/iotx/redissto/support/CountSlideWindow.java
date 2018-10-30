package com.aliyun.iotx.redissto.support;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 容量 滑动窗口
 *
 * @author jiehong.jh
 * @date 2018/4/27
 */
public class CountSlideWindow<T> extends AbstractSlideWindow<T> {

    private final int max;
    protected AtomicInteger count = new AtomicInteger();

    /**
     * @param length bucket个数
     * @param head
     * @param max    每个bucket容量
     */
    public CountSlideWindow(int length, AbstractBucket<T> head, int max) {
        super(length, head);
        this.max = max;
    }

    @Override
    public void put(T t) {
        rLock.lock();
        if (count.getAndIncrement() < max) {
            arr[index].onAccept(t);
            rLock.unlock();
        } else {
            rLock.unlock();
            wLock.lock();
            try {
                if (count.get() >= max) {
                    count.set(0);
                    safeKeepNext();
                }
                count.getAndIncrement();
                arr[index].onAccept(t);
            } finally {
                wLock.unlock();
            }
        }
    }

    @Override
    public void close() throws Exception {
    }
}
