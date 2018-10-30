package com.aliyun.iotx.redissto.function;

import java.util.concurrent.Callable;

import org.redisson.api.RFuture;

/**
 * redisson <code>RFuture</code>
 *
 * @author jiehong.jh
 * @date 2018/8/3
 */
@FunctionalInterface
public interface AsyncCallable<V> extends Callable<RFuture<V>> {

    /**
     * Computes a result
     *
     * @return computed result
     */
    @Override
    RFuture<V> call();
}
