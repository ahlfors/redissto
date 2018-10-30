package com.aliyun.iotx.redissto.failover;

import com.aliyun.iotx.redissto.metrics.ClusterSynchronizer;

/**
 * @author jiehong.jh
 * @date 2018/8/16
 */
public interface NativeFailoverSynchronizer extends ClusterSynchronizer {

    /**
     * Save the redis keys to be deleted
     *
     * @param redisKeys
     */
    void failover(String... redisKeys);
}
