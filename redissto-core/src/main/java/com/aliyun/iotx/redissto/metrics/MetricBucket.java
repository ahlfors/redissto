package com.aliyun.iotx.redissto.metrics;

import com.aliyun.iotx.redissto.support.AbstractBucket;

/**
 * @author jiehong.jh
 * @date 2018/8/1
 */
public class MetricBucket extends AbstractBucket<Boolean> {

    /**
     * share status throughout the slide window
     */
    private MetricStatus status;
    private int okPermits;

    public MetricBucket(MetricStatus status, int okPermits) {
        super(new MetricContainer(status, okPermits));
        this.status = status;
        this.okPermits = okPermits;
    }

    @Override
    public AbstractBucket<Boolean> next() {
        return new MetricBucket(status, okPermits);
    }
}
