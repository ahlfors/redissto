package com.aliyun.iotx.redissto.metrics;

import com.aliyun.iotx.redissto.failover.AbstractClusterSynchronizer;

/**
 * Native watchdog <p>#Thread safe#</p>
 *
 * @author jiehong.jh
 * @date 2018/8/13
 * @see AbstractClusterSynchronizer
 */
public interface NativeWatchdog {

    /**
     * try reconnect
     *
     * @return
     */
    boolean tryReconnect();

    /**
     * try reconnect ok and start recovery
     *
     * @return
     */
    boolean doRecovery();
}
