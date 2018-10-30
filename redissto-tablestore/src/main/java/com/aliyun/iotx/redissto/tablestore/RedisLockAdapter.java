package com.aliyun.iotx.redissto.tablestore;

import java.util.concurrent.TimeUnit;

import com.aliyun.iotx.fluentable.api.GenericLock;
import org.redisson.api.RLock;

/**
 * Redis Lock Adapter
 *
 * @author jiehong.jh
 * @date 2018/9/20
 */
public class RedisLockAdapter implements GenericLock {

    private RLock rLock;

    public RedisLockAdapter(RLock rLock) {
        this.rLock = rLock;
    }

    @Override
    public boolean tryLock(long leaseTime, TimeUnit unit) {
        try {
            return rLock.tryLock(leaseTime, unit);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        return rLock.tryLock(waitTime, leaseTime, unit);
    }

    @Override
    public void lock(long leaseTime, TimeUnit unit) {
        rLock.lock(leaseTime, unit);
    }

    @Override
    public void unlock() {
        rLock.unlock();
    }
}
