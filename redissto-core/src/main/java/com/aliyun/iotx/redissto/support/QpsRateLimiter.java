package com.aliyun.iotx.redissto.support;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.extern.slf4j.Slf4j;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * QpsRateLimiter
 *
 * @author jiehong.jh
 * @date 2018/7/6
 */
@Slf4j
public class QpsRateLimiter implements RateLimiter {
    private final long duration;
    private final int maxPermits;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private long[] bucket;
    private AtomicInteger index = new AtomicInteger();

    public QpsRateLimiter(long time, TimeUnit unit, int maxPermits) {
        this.duration = unit.toNanos(time);
        this.maxPermits = maxPermits;
        this.bucket = new long[maxPermits];
    }

    @Override
    public void acquire() {
        long sleep;
        rwl.readLock().lock();
        int i = index.getAndIncrement();
        if (i < maxPermits) {
            sleep = doAcquire(i);
            rwl.readLock().unlock();
        } else {
            rwl.readLock().unlock();
            rwl.writeLock().lock();
            try {
                safeGetIndex();
                sleep = doAcquire();
            } finally {
                rwl.writeLock().unlock();
            }
        }
        SleepUtils.block(sleep, TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(0, NANOSECONDS);
    }

    @Override
    public boolean tryAcquire(long time, TimeUnit unit) {
        long nanos = unit.toNanos(time);
        rwl.readLock().lock();
        int i = index.get();
        if (i < maxPermits && bucket[i] - nanos > System.nanoTime()) {
            rwl.readLock().unlock();
            return false;
        }
        rwl.readLock().unlock();
        long sleep = 0;
        rwl.writeLock().lock();
        try {
            if (bucket[safeGetIndex()] - nanos > System.nanoTime()) {
                return false;
            }
            sleep = doAcquire();
            return true;
        } finally {
            rwl.writeLock().unlock();
            SleepUtils.block(sleep, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * get index in safe operation
     *
     * @return
     */
    private int safeGetIndex() {
        if (index.get() >= maxPermits) {
            index.set(0);
        }
        return index.get();
    }

    private long doAcquire() {
        return doAcquire(index.getAndIncrement());
    }

    /**
     * do acquire without block lock release
     *
     * @param i
     * @return
     */
    private long doAcquire(int i) {
        long now = System.nanoTime();
        if (bucket[i] <= now) {
            bucket[i] = now + duration;
            return 0;
        }
        long sleep = bucket[i] - now;
        bucket[i] = bucket[i] + duration;
        return sleep;
    }
}
