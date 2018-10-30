package com.aliyun.iotx.redissto;

/**
 * @author jiehong.jh
 * @date 2018/10/26
 */
@FunctionalInterface
public interface DualKey {

    /**
     * 获取双写Key
     *
     * @return
     */
    String value();
}
