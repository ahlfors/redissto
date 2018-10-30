package com.aliyun.iotx.redissto.eventbus;

import java.util.concurrent.TimeUnit;

/**
 * 支持集群消费
 *
 * @author jiehong.jh
 * @date 2018/10/17
 */
public interface ClusterEventBus extends EventBus {

    /**
     * 单播延迟消息到该Topic下的其中一个订阅者
     *
     * @param topic   Topic 必须要存在当前应用内
     * @param message
     * @param delay
     * @param unit
     */
    <T> void delivery(String topic, T message, long delay, TimeUnit unit);
}
