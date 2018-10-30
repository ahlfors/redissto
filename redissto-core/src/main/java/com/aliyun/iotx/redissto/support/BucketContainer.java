package com.aliyun.iotx.redissto.support;

/**
 * 实现请保证线程安全
 *
 * @author jiehong.jh
 * @date 2018/4/27
 */
public interface BucketContainer<T> {

    /**
     * accept object
     *
     * @param t 收到时触发
     */
    void onAccept(T t);

    /**
     * 当前bucket被滑动窗口切换时触发，保证只会触发一次（线程安全）
     *
     * @param ttl bucket的存活时间
     */
    void onComplete(long ttl);

    /**
     * bucket过期时触发，保证只会触发一次（线程安全）
     */
    void onExpire();
}
