package com.aliyun.iotx.redissto.support;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 滑动窗口（线程安全）
 *
 * @author jiehong.jh
 * @date 2018/4/26
 */
public abstract class AbstractSlideWindow<T> implements SlideWindow<T> {

    protected final AbstractBucket<T>[] arr;
    /**
     * bucket number
     */
    protected final int length;
    private final ReentrantReadWriteLock bucketLock = new ReentrantReadWriteLock();
    protected final Lock rLock = bucketLock.readLock();
    protected final Lock wLock = bucketLock.writeLock();
    /**
     * current bucket index
     */
    protected volatile int index;
    /**
     * current bucket creation time
     */
    protected long liveTime;

    /**
     * @param length bucket个数
     */
    @SuppressWarnings("unchecked")
    public AbstractSlideWindow(int length, AbstractBucket<T> head) {
        this.length = length;
        arr = (AbstractBucket<T>[])new AbstractBucket[length];
        arr[0] = head;
        liveTime = System.currentTimeMillis();
    }

    /**
     * 当前bucket切换，保持滑动窗口连续
     */
    protected void safeKeepNext() {
        wLock.lock();
        try {
            AbstractBucket<T> bucket = arr[index];
            if (index == length - 1) {
                index = 0;
            } else {
                index++;
            }
            if (arr[index] != null) {
                arr[index].onExpire();
            }
            arr[index] = bucket.next();
            long now = System.currentTimeMillis();
            long ttl = now - liveTime;
            liveTime = now;
            onSafeNext();
            bucket.onComplete(ttl);
        } finally {
            wLock.unlock();
        }
    }

    /**
     * 切换新的bucket时触发
     */
    protected void onSafeNext() {
    }

}