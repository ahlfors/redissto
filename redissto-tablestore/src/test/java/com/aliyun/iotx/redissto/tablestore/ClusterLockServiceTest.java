package com.aliyun.iotx.redissto.tablestore;

import java.util.concurrent.TimeUnit;

import com.aliyun.iotx.fluentable.FluentableService;
import com.aliyun.iotx.fluentable.api.GenericLock;
import com.aliyun.iotx.fluentable.api.GenericQueue;
import com.aliyun.iotx.fluentable.api.QueueElement;
import com.aliyun.iotx.redissto.RedisExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author jiehong.jh
 * @date 2018/9/23
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JunitConfiguration.class)
public class ClusterLockServiceTest {

    @Autowired
    private RedisExecutor redisExecutor;
    @Autowired
    private RedissonClient redisson;
    @Autowired
    private FluentableService fluentableService;
    @Autowired
    private ClusterLockService clusterLockService;

    private GenericQueue<String> queue;

    @Before
    public void setUp() {
        queue = fluentableService.opsForQueue("key1");
    }

    @Test
    public void poll() {
        new Thread(() -> {
            System.out.println("redis runner start..");
            while (true) {
                RBucket<String> bucket = redisson.getBucket("hello");
                redisExecutor.asyncGet(bucket::getAsync);
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        GenericLock lock = clusterLockService.getLock("queue:key1");
        while (true) {
            try {
                lock.lock(3, TimeUnit.SECONDS);
                System.out.println("-----start poll-----");
                QueueElement<String> element = queue.peek();
                Long sequence = element.getSequence();
                System.out.println(sequence + ":" + queue.remove(sequence));
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                System.out.println("poll error: " + e.getMessage());
            } finally {
                lock.unlock();
            }
        }
    }

}
