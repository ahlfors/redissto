package com.aliyun.iotx.redissto.eventbus;

/**
 * A generic event handler.
 *
 * @author jiehong.jh
 * @date 2018/10/15
 */
@FunctionalInterface
public interface EventHandler<T> {

    /**
     * Something has happened, so handle it.
     *
     * @param event
     */
    void handle(Event<T> event);
}