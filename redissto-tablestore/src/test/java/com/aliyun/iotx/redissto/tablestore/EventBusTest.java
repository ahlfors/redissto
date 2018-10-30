package com.aliyun.iotx.redissto.tablestore;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.aliyun.iotx.redissto.eventbus.ClusterEventBus;
import com.aliyun.iotx.redissto.eventbus.EventHandler;
import com.aliyun.iotx.redissto.eventbus.EventTopic;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RList;
import org.redisson.api.RScript.Mode;
import org.redisson.api.RScript.ReturnType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author jiehong.jh
 * @date 2018/10/15
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JunitConfiguration.class, EventBusConfiguration.class})
public class EventBusTest {

    public static final String TOPIC = "HomJieTopic";
    @Autowired
    private ClusterEventBus eventBus;
    @Autowired
    private RedissonClient redissonClient;

    public static EventHandler<String> handler() {
        return event -> {
            String value = event.body();
            if (value.startsWith("Delay")) {
                long millis = System.currentTimeMillis();
                String actual = value.substring(5);
                long actualDelay = millis - Long.parseLong(actual);
                log.info("current: {}, actual: {}, delay: {}", millis, actual, actualDelay);
            } else {
                log.info("event: {}", value);
            }
        };
    }

    @Test
    public void script() {
        String name = "HomJie:List:001";
        RList<String> list = redissonClient.getList(name);
        list.add("Hello");
        list.add("World");
        System.out.println(list.isExists());
        String script = "local items = redis.call('lrange', KEYS[1], 0, -1); "
            + "redis.call('del', KEYS[1]); "
            + "return items; ";
        List<String> bucket = redissonClient.getScript().eval(Mode.READ_WRITE,
            script, ReturnType.MULTI, Collections.singletonList(name));
        System.out.println(list.isExists());
        System.out.println(bucket);
        bucket = redissonClient.getScript().eval(Mode.READ_WRITE,
            script, ReturnType.MULTI, Collections.singletonList(name));
        System.out.println(list.isExists());
        System.out.println(bucket);
    }

    @Test
    public void publish() {
        String message = "Hello";
        eventBus.publish(TOPIC, message);
        log.info("publish: {}", message);
    }

    @Test
    public void publishDelay() throws InterruptedException {
        String message = "Delay" + System.currentTimeMillis();
        eventBus.publish(TOPIC, message, 2, TimeUnit.SECONDS);
        log.info("publish: {}", message);
        // 延迟发布需要等待定时调度
        TimeUnit.SECONDS.sleep(3);
    }

    @Test
    public void batchPublishDelay() throws InterruptedException {
        String message = "Delay" + System.currentTimeMillis();
        long start = System.currentTimeMillis();
        eventBus.publish(TOPIC, message, 1, TimeUnit.SECONDS);
        log.info("First exec: {}", (System.currentTimeMillis() - start));
        TimeUnit.SECONDS.sleep(2);
        int loop = 500;
        while (loop-- > 0) {
            start = System.currentTimeMillis();
            eventBus.publish(TOPIC, "Delay" + start, 280, TimeUnit.MILLISECONDS);
            log.info("exec: {}", (System.currentTimeMillis() - start));
            // 屏蔽 sleep 模拟并发
            //TimeUnit.MILLISECONDS.sleep(120);
        }
        // 延迟发布需要等待定时调度
        TimeUnit.SECONDS.sleep(10);
    }

    @Test
    public void batchDeliveryDelay() throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        String message = "Delay" + System.currentTimeMillis();
        long start = System.currentTimeMillis();
        eventBus.delivery(TOPIC, message, 1, TimeUnit.SECONDS);
        log.info("First exec: {}", (System.currentTimeMillis() - start));
        TimeUnit.SECONDS.sleep(2);
        int loop = 500;
        while (loop-- > 0) {
            start = System.currentTimeMillis();
            eventBus.delivery(TOPIC, "Delay" + start, 280, TimeUnit.MILLISECONDS);
            log.info("exec: {}", (System.currentTimeMillis() - start));
            // 屏蔽 sleep 模拟并发
            //TimeUnit.MILLISECONDS.sleep(120);
        }
        // 延迟发布需要等待定时调度
        TimeUnit.SECONDS.sleep(10);
    }

    @Test
    public void consumer() throws IOException {
        eventBus.consumer(EventTopic.of(TOPIC), handler());
        System.in.read();
    }

    @Test
    public void consumerPattern() throws IOException {
        eventBus.consumer(EventTopic.pattern("HomJie*"), handler());
        System.in.read();
    }
}
