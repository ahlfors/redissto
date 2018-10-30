package com.aliyun.iotx.redissto.support;

import java.util.concurrent.TimeUnit;

/**
 * RateLimiter
 *
 * @author jiehong.jh
 * @date 2018/7/6
 */
public interface RateLimiter {

    /**
     * Acquires the permit.
     */
    void acquire();

    /**
     * Acquires the permit only if it is free at the time of invocation.
     *
     * @return {@code true} if the permit was acquired and {@code false} otherwise
     */
    boolean tryAcquire();

    /**
     * Acquires the permit if it is free within the given waiting time.
     *
     * @param time the maximum time to wait for the permit
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the permit was acquired and {@code false} if the waiting time elapsed before the permit
     * was acquired
     */
    boolean tryAcquire(long time, TimeUnit unit);
}
