package com.aliyun.iotx.redissto.metrics;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jiehong.jh
 * @date 2018/8/1
 */
@Getter
@Setter
public class MetricProperties {
    /**
     * 断路器检查间隔
     */
    private long time;
    /**
     * 断路器检查间隔单位
     */
    private TimeUnit unit;
    /**
     * 断路器处于半开状态时，所允许通过的请求个数
     */
    private int permits;
    /**
     * 断路器处于半开状态时，所通过的成功请求个数，用于快速恢复，关闭断路器
     */
    private int okPermits;
    /**
     * 断路器半开状态时的成功比例
     */
    private int warnThreshold;
    /**
     * 断路器全开状态时的成功比例
     */
    private int errorThreshold;
}
