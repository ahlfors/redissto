package com.aliyun.iotx.redissto.eventbus;

import java.util.concurrent.TimeUnit;

/**
 * 支持广播消费
 *
 * @author jiehong.jh
 * @date 2018/10/15
 */
public interface EventBus extends AutoCloseable {
    /**
     * 广播消息到该Topic下的所有订阅者
     *
     * @param topic
     * @param message
     */
    <T> void publish(String topic, T message);

    /**
     * 广播延迟消息到该Topic下的所有订阅者
     *
     * @param topic
     * @param message
     * @param delay
     * @param unit
     */
    <T> void publish(String topic, T message, long delay, TimeUnit unit);

    /**
     * 消费指定Topic的消息
     *
     * @param eventTopic 支持通配符
     * @param handler    Not block the redisson thread pool
     */
    <T> void consumer(EventTopic eventTopic, EventHandler<T> handler);

    /**
     * 关闭 EventBus
     */
    @Override
    void close();
}
