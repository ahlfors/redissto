package com.aliyun.iotx.redissto.metrics;

/**
 * Circuit Breaker
 *
 * @author jiehong.jh
 * @date 2018/8/1
 */
public interface CircuitBreaker extends AutoCloseable {

    /**
     * Every requests asks this if it is allowed to proceed or not. Allow to pass some requests when it is half open.
     *
     * @return boolean whether a request should be permitted
     */
    boolean allowRequest();

    /**
     * Whether the circuit is currently close (tripped).
     *
     * @return boolean state of circuit breaker
     */
    boolean isClose();

    /**
     * Invoked on successful executions.
     */
    void markSuccess();

    /**
     * Invoked on unsuccessful executions.
     */
    void markNonSuccess();
}
