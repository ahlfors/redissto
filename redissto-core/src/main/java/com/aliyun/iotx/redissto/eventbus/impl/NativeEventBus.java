package com.aliyun.iotx.redissto.eventbus.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.aliyun.iotx.redissto.support.SleepUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RFuture;
import org.redisson.api.RTopic;

/**
 * A redis event bus implementation by delay
 *
 * @author jiehong.jh
 * @date 2018/10/15
 * @see BucketEventBus
 */
public class NativeEventBus extends AbstractEventBus {

    /**
     * 延时队列前缀
     */
    private static final String DELAY = "DELAY:";
    /**
     * 增加队列个数来提高调度精度
     */
    private static final int CONCURRENT = 0xF;

    private ConcurrentMap<String, List<RDelayedQueue<?>>> queueCache = new ConcurrentHashMap<>();

    public NativeEventBus(ExecutorService executor) {
        super(executor);
        logger.info("NativeEventBus start success.");
    }

    private String prefixDelay(String topic, long serial) {
        return DELAY + serial + ":" + topic;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void publish(String topic, T message, long delay, TimeUnit unit) {
        if (topic == null || message == null) {
            return;
        }
        if (delay <= 0) {
            publish(topic, message);
            return;
        }
        List<RDelayedQueue<?>> delayedQueueList = queueCache.computeIfAbsent(topic, name -> {
            final List<RBlockingQueue<T>> queueList = new ArrayList<>(CONCURRENT + 1);
            for (int i = 0; i <= CONCURRENT; i++) {
                queueList.add(redissonClient.getBlockingQueue(prefixDelay(topic, i)));
            }
            // Note: 每一个延时队列都将消耗一个线程池
            executor.execute(() -> {
                RTopic<T> rTopic = redissonClient.getTopic(topic);
                Map<Integer, RFuture<T>> futureMap = new HashMap<>(CONCURRENT + 1);
                for (int i = 0; i <= CONCURRENT; i++) {
                    futureMap.put(i, queueList.get(i).takeAsync());
                }
                for (; ; ) {
                    boolean allWait = true;
                    for (Entry<Integer, RFuture<T>> entry : futureMap.entrySet()) {
                        int i = entry.getKey();
                        RFuture<T> future = entry.getValue();
                        if (future.isDone()) {
                            allWait = false;
                            rTopic.publishAsync(future.join());
                            futureMap.put(i, queueList.get(i).takeAsync());
                        }
                    }
                    if (allWait) {
                        SleepUtils.block(10, TimeUnit.MILLISECONDS);
                    }
                }
            });
            return queueList.stream().map(redissonClient::getDelayedQueue).collect(Collectors.toList());
        });
        int serial = (int)(System.currentTimeMillis() & CONCURRENT);
        ((RDelayedQueue<T>)delayedQueueList.get(serial)).offerAsync(message, delay, unit);
    }

    @Override
    public void close() {
        logger.info("NativeEventBus wait close.");
        executor.shutdown();
    }
}
