package com.aliyun.iotx.redissto.support;

/**
 * 滑动窗口
 *
 * @author jiehong.jh
 * @date 2018/8/1
 */
public interface SlideWindow<T> extends AutoCloseable {

    /**
     * put object
     *
     * @param t 保证只被当前bucket处理
     */
    void put(T t);
}
