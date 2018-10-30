package com.aliyun.iotx.redissto.metrics;

/**
 * @author jiehong.jh
 * @date 2018/8/13
 */
public interface ClusterSynchronizer extends AutoCloseable {

    /**
     * The storage synchronizer start
     */
    void start();

    /**
     * Associated with the state of the storage
     *
     * @return
     */
    boolean allowRead();

    /**
     * Associated with its own state
     *
     * @return
     */
    boolean allowWrite();

    /**
     * Invoked on successful executions.
     */
    void markSuccess();

    /**
     * Invoked on unsuccessful executions.
     */
    void markNonSuccess();

    /**
     * Broadcast native state
     *
     * @param nativeState
     */
    void broadcast(boolean nativeState);
}
