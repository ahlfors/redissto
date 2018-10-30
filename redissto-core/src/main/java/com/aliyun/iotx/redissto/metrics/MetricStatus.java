package com.aliyun.iotx.redissto.metrics;

/**
 * Metric Status
 *
 * @author jiehong.jh
 * @date 2018/8/1
 */
public interface MetricStatus {

    /**
     * Update the native state by ratio
     *
     * @param ratio
     */
    void setState(int ratio);

    /**
     * Not call or in fully open
     */
    void setState();

    /**
     * The native state is half-open
     *
     * @return
     */
    boolean isHalfOpen();

    /**
     * Make the native state to close
     */
    void close();
}
