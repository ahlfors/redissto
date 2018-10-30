package com.aliyun.iotx.redissto.tablestore;

import java.util.concurrent.TimeUnit;

import com.aliyun.iotx.fluentable.FluentableService;
import com.aliyun.iotx.fluentable.api.GenericLock;
import com.aliyun.iotx.redissto.failover.NativeFailoverSynchronizer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 组合Redis和TableStore的锁
 *
 * @author jiehong.jh
 * @date 2018/9/23
 */
@Slf4j
public class ClusterLockService {

    private FluentableService fluentableService;
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private NativeFailoverSynchronizer synchronizer;

    public ClusterLockService(FluentableService fluentableService) {
        this.fluentableService = fluentableService;
    }

    /**
     * 获取分布式锁
     *
     * @return 分布式锁
     */
    public GenericLock getLock(String name) {
        GenericLock redisLock = new RedisLockAdapter(redissonClient.getLock(name));
        GenericLock tableStoreLock = fluentableService.opsForLock(name);
        return new DynamicLock(redisLock, tableStoreLock);
    }

    /**
     * 动态锁（Redis不可用时自动降级为TableStore）
     */
    private class DynamicLock implements GenericLock {

        private GenericLock redisLock;
        private GenericLock tableStoreLock;

        private ThreadLocal<GenericLock> lookup = new ThreadLocal<>();

        DynamicLock(GenericLock redisLock, GenericLock tableStoreLock) {
            this.redisLock = redisLock;
            this.tableStoreLock = tableStoreLock;
        }

        private GenericLock get() {
            if (synchronizer.allowRead()) {
                lookup.set(redisLock);
                return redisLock;
            }
            log.warn("The current lock is implemented by TableStore");
            lookup.set(tableStoreLock);
            return tableStoreLock;
        }

        @Override
        public boolean tryLock(long leaseTime, TimeUnit unit) {
            return get().tryLock(leaseTime, unit);
        }

        @Override
        public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
            return get().tryLock(waitTime, leaseTime, unit);
        }

        @Override
        public void lock(long leaseTime, TimeUnit unit) {
            get().lock(leaseTime, unit);
        }

        @Override
        public void unlock() {
            try {
                lookup.get().unlock();
            } catch (Exception e) {
                log.error("unlock error", e);
            } finally {
                lookup.remove();
            }
        }
    }
}
