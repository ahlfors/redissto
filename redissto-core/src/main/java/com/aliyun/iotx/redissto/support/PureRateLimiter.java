package com.aliyun.iotx.redissto.support;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PureRateLimiter
 * <p>
 * only has 1 permit
 *
 * @author jiehong.jh
 * @date 2018/8/3
 */
public class PureRateLimiter implements RateLimiter {

    private final long duration;
    private AtomicLong timeNanos = new AtomicLong();

    public PureRateLimiter(long time, TimeUnit unit) {
        this.duration = unit.toNanos(time);
    }

    @Override
    public void acquire() {
        while (true) {
            long expired = timeNanos.get();
            long current = System.nanoTime();
            SleepUtils.block(expired + duration - current, TimeUnit.NANOSECONDS);
            if (timeNanos.compareAndSet(expired, System.nanoTime())) {
                return;
            }
        }
    }

    @Override
    public boolean tryAcquire() {
        long expired = timeNanos.get();
        long current = System.nanoTime();
        return current >= expired + duration && timeNanos.compareAndSet(expired, current);
    }

    @Override
    public boolean tryAcquire(long time, TimeUnit unit) {
        long expired = timeNanos.get();
        long current = System.nanoTime();
        final long deadline = current + unit.toNanos(time);
        while (current <= deadline && deadline >= expired + duration) {
            SleepUtils.block(expired + duration - current, TimeUnit.NANOSECONDS);
            current = System.nanoTime();
            if (timeNanos.compareAndSet(expired, current)) {
                return true;
            }
            expired = timeNanos.get();
        }
        return false;
    }
}
