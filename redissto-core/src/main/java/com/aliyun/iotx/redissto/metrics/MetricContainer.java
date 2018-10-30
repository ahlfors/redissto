package com.aliyun.iotx.redissto.metrics;

import java.util.concurrent.atomic.AtomicInteger;

import com.aliyun.iotx.redissto.support.BucketContainer;

/**
 * @author jiehong.jh
 * @date 2018/8/1
 */
public class MetricContainer implements BucketContainer<Boolean> {

    private static final int ALL_FAILURE = 0;
    private static final int ALL_SUCCESS = 100;

    private MetricStatus status;
    private int okPermits;
    /**
     * success counter
     */
    private AtomicInteger successCounter = new AtomicInteger();
    /**
     * failure counter
     */
    private AtomicInteger failureCounter = new AtomicInteger();

    public MetricContainer(MetricStatus status, int okPermits) {
        this.status = status;
        this.okPermits = okPermits;
    }

    @Override
    public void onAccept(Boolean boo) {
        if (boo) {
            int success = successCounter.incrementAndGet();
            if (status.isHalfOpen() && success == okPermits) {
                status.close();
            }
        } else {
            failureCounter.incrementAndGet();
        }
    }

    @Override
    public void onComplete(long ttl) {
        int failure = failureCounter.get();
        int success = successCounter.get();
        if (failure == 0 && success == 0) {
            status.setState();
            return;
        }
        if (failure == 0) {
            status.setState(ALL_SUCCESS);
            return;
        }
        if (success == 0) {
            status.setState(ALL_FAILURE);
            return;
        }
        int ratio = success * 100 / (success + failure);
        status.setState(ratio);
    }

    @Override
    public void onExpire() {
    }
}
