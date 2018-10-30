package com.aliyun.iotx.redissto;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.aliyun.iotx.redissto.failover.EmptyFailoverSynchronizer;
import com.aliyun.iotx.redissto.failover.NativeFailoverSynchronizer;
import com.aliyun.iotx.redissto.metrics.CircuitBreakerImpl;
import com.aliyun.iotx.redissto.metrics.MetricProperties;
import com.aliyun.iotx.redissto.support.SimpleThreadFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jiehong.jh
 * @date 2018/8/7
 */
@Configuration
public class RedisstoAutoConfiguration {

    @Bean(initMethod = "start")
    @ConditionalOnMissingBean
    public NativeFailoverSynchronizer nativeFailoverSynchronizer() {
        MetricProperties properties = new MetricProperties();
        properties.setTime(5);
        properties.setUnit(TimeUnit.SECONDS);
        properties.setWarnThreshold(75);
        properties.setErrorThreshold(50);
        properties.setPermits(10);
        properties.setOkPermits(8);
        CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(properties);
        return new EmptyFailoverSynchronizer(circuitBreaker);
    }

    @Bean
    public RedisExecutor redisExecutor(NativeFailoverSynchronizer nativeFailoverSynchronizer) {
        return new RedisExecutor(nativeFailoverSynchronizer);
    }

    @Bean
    @ConditionalOnMissingBean
    public DualClient dualClient() {
        int nThreads = Runtime.getRuntime().availableProcessors() << 1;
        return new DualClient().asyncPool(new ThreadPoolExecutor(nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new SimpleThreadFactory("dual-pool")));
    }
}
