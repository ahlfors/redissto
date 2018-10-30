package com.aliyun.iotx.redissto.failover;

import com.aliyun.iotx.redissto.metrics.CircuitBreaker;

/**
 * Stand-alone switch always default open
 *
 * @author jiehong.jh
 * @date 2018/8/13
 */
public final class EmptyFailoverSynchronizer implements NativeFailoverSynchronizer {

    private CircuitBreaker circuitBreaker;

    public EmptyFailoverSynchronizer(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void start() {
        // do nothing
    }

    @Override
    public void close() throws Exception {
        circuitBreaker.close();
    }

    @Override
    public boolean allowRead() {
        return circuitBreaker.allowRequest();
    }

    @Override
    public boolean allowWrite() {
        return circuitBreaker.allowRequest();
    }

    @Override
    public void markSuccess() {
        circuitBreaker.markSuccess();
    }

    @Override
    public void markNonSuccess() {
        circuitBreaker.markNonSuccess();
    }

    @Override
    public void broadcast(boolean nativeState) {
        // do nothing
    }

    @Override
    public void failover(String... redisKeys) {
        // do nothing
    }
}
