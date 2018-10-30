package com.aliyun.iotx.redissto.eventbus;

/**
 * @author jiehong.jh
 * @date 2018/10/15
 */
public interface Event<T> {
    /**
     * The event topic
     *
     * @return
     */
    String topic();

    /**
     * The event body
     *
     * @return
     */
    T body();
}
