package com.aliyun.iotx.redissto.eventbus;

/**
 * @author jiehong.jh
 * @date 2018/10/15
 */
public class EventImpl<T> implements Event<T> {

    private String topic;
    private T body;

    public EventImpl(String topic, T body) {
        this.topic = topic;
        this.body = body;
    }

    @Override
    public String topic() {
        return topic;
    }

    @Override
    public T body() {
        return body;
    }
}
