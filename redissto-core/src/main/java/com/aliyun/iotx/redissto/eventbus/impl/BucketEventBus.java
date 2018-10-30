package com.aliyun.iotx.redissto.eventbus.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.aliyun.iotx.redissto.eventbus.ClusterEventBus;
import com.aliyun.iotx.redissto.eventbus.Event;
import com.aliyun.iotx.redissto.eventbus.EventHandler;
import com.aliyun.iotx.redissto.eventbus.EventImpl;
import com.aliyun.iotx.redissto.support.SimpleThreadFactory;
import com.aliyun.iotx.redissto.support.SleepUtils;
import org.redisson.api.RScript.Mode;
import org.redisson.api.RScript.ReturnType;

/**
 * A redis event bus implementation by bucket
 * <p>
 * More excellent than {@link NativeEventBus}
 *
 * @author jiehong.jh
 * @date 2018/10/16
 */
public class BucketEventBus extends AbstractEventBus implements ClusterEventBus {

    private static final String BUCKET = "BUCKET:";
    /**
     * Atom acquire list
     */
    private static final String SCRIPT = "local items = redis.call('lrange', KEYS[1], 0, -1); "
        + "redis.call('del', KEYS[1]); "
        + "return items; ";

    private int coreThreads;
    private int precision;
    private int deviation;
    private int period;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    private Map<String, EventHandler<?>> clusterTopicConfig;

    private ScheduledExecutorService scheduler;

    public BucketEventBus(BucketEventBusOption option, ExecutorService executor) {
        super(executor);
        this.coreThreads = option.getCoreThreads();
        this.precision = option.getPrecision();
        this.period = coreThreads * precision;
        this.deviation = option.getDeviation();
        this.clusterTopicConfig = option.getClusterTopicConfig();
    }

    @PostConstruct
    private void initialize() {
        scheduler = new ScheduledThreadPoolExecutor(coreThreads, new SimpleThreadFactory("BucketEventBus"));
        Runnable command = () -> {
            try {
                // Note: 在高精度情况下，考虑 I/O 通信耗时，执行提前调度
                long index = System.currentTimeMillis() / precision;
                List<BucketElement> bucket = redissonClient.getScript().eval(Mode.READ_WRITE,
                    SCRIPT, ReturnType.MULTI, Collections.singletonList(BUCKET + index));
                bucket.forEach(e -> {
                    String topic = e.getTopic();
                    Object message = e.getMessage();
                    if (e.isCluster()) {
                        EventHandler<?> handler = clusterTopicConfig.get(topic);
                        if (handler == null) {
                            logger.warn("Topic: {}, message: {} is discard.");
                        } else {
                            delivery(handler, new EventImpl<>(topic, message));
                        }
                    } else {
                        publish(topic, message);
                    }
                });
            } catch (Exception e) {
                logger.error("BucketEventBus internal error", e);
            }
        };
        long now = System.currentTimeMillis();
        long r = now % precision;
        long initialDelay = r == 0 ? 0 : precision - r;
        for (int i = 0; i < coreThreads; i++) {
            scheduler.scheduleAtFixedRate(command, initialDelay + i * precision, period, timeUnit);
        }
        SleepUtils.block(initialDelay, timeUnit);
        logger.info("BucketEventBus start success.");
    }

    @Override
    public <T> void publish(String topic, T message, long delay, TimeUnit unit) {
        long millis = unit.toMillis(delay);
        if (millis <= deviation) {
            // Note: 如果并发非常大使得耗尽 Redisson 线程池，将导致延迟突增
            scheduler.schedule(() -> publish(topic, message), delay, unit);
            return;
        }
        long index = (System.currentTimeMillis() + millis) / precision;
        BucketElement element = new BucketElement(topic, message, false);
        redissonClient.getList(BUCKET + index).add(element);
    }

    @Override
    public <T> void delivery(String topic, T message, long delay, TimeUnit unit) {
        EventHandler<?> handler = clusterTopicConfig.get(topic);
        if (handler == null) {
            logger.warn("Topic: {}, message: {} is discard.");
            return;
        }
        long millis = unit.toMillis(delay);
        if (millis <= deviation) {
            // Note: 延迟时间非常短则执行本地调度
            scheduler.schedule(() -> delivery(handler, new EventImpl<>(topic, message)), delay, unit);
            return;
        }
        long index = (System.currentTimeMillis() + millis) / precision;
        BucketElement element = new BucketElement(topic, message, true);
        redissonClient.getList(BUCKET + index).add(element);
    }

    @SuppressWarnings("unchecked")
    private void delivery(EventHandler<?> handler, Event event) {
        executor.execute(() -> handler.handle(event));
    }

    @Override
    public void close() {
        logger.info("BucketEventBus wait close.");
        SleepUtils.block(deviation, timeUnit);
        scheduler.shutdown();
        executor.shutdown();
    }
}
