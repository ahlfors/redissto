package com.aliyun.iotx.redissto.eventbus.impl;

import java.util.concurrent.ExecutorService;

import com.aliyun.iotx.redissto.eventbus.EventBus;
import com.aliyun.iotx.redissto.eventbus.EventHandler;
import com.aliyun.iotx.redissto.eventbus.EventImpl;
import com.aliyun.iotx.redissto.eventbus.EventTopic;
import org.redisson.api.RPatternTopic;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.listener.PatternMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jiehong.jh
 * @date 2018/10/16
 */
public abstract class AbstractEventBus implements EventBus {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected ExecutorService executor;
    @Autowired
    protected RedissonClient redissonClient;

    /**
     * @param executor Worker Thread Pool
     */
    public AbstractEventBus(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public <T> void publish(String topic, T message) {
        if (topic == null || message == null) {
            return;
        }
        redissonClient.getTopic(topic).publishAsync(message);
    }

    @Override
    public <T> void consumer(EventTopic eventTopic, EventHandler<T> handler) {
        String topic;
        if (eventTopic == null || (topic = eventTopic.getName()) == null) {
            return;
        }
        if (eventTopic.isPattern()) {
            RPatternTopic<T> patternTopic = redissonClient.getPatternTopic(topic);
            patternTopic.addListener(new Listener<>(handler));
        } else {
            RTopic<T> rTopic = redissonClient.getTopic(topic);
            rTopic.addListener(new Listener<>(handler));
        }
    }

    private class Listener<M> implements MessageListener<M>, PatternMessageListener<M> {

        private EventHandler<M> handler;

        private Listener(EventHandler<M> handler) {
            this.handler = handler;
        }

        @Override
        public void onMessage(CharSequence channel, M msg) {
            executor.execute(() -> handler.handle(new EventImpl<>(channel.toString(), msg)));
        }

        @Override
        public void onMessage(CharSequence pattern, CharSequence channel, M msg) {
            executor.execute(() -> handler.handle(new EventImpl<>(channel.toString(), msg)));
        }
    }
}
