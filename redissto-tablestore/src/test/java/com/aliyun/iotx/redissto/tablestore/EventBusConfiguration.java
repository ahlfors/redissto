package com.aliyun.iotx.redissto.tablestore;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.aliyun.iotx.redissto.eventbus.ClusterEventBus;
import com.aliyun.iotx.redissto.eventbus.EventHandler;
import com.aliyun.iotx.redissto.eventbus.impl.BucketEventBus;
import com.aliyun.iotx.redissto.eventbus.impl.BucketEventBusOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jiehong.jh
 * @date 2018/10/10
 */
@Configuration
public class EventBusConfiguration {

    //@Bean
    //public EventBus eventBus() {
    //    ExecutorService executor = Executors.newFixedThreadPool(20);
    //    return new NativeEventBus(executor);
    //}

    @Bean
    public ClusterEventBus eventBus() {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        BucketEventBusOption option = new BucketEventBusOption();
        Map<String, EventHandler<?>> config = option.getClusterTopicConfig();
        config.put(EventBusTest.TOPIC, EventBusTest.handler());
        return new BucketEventBus(option, executor);
    }
}
