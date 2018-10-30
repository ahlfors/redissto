package com.aliyun.iotx.redissto;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jiehong.jh
 * @date 2018/10/10
 */
@Getter
@Setter
public class DualMonitorOption {
    /**
     * 检查间隔
     */
    private long time;
    /**
     * 间隔单位
     */
    private TimeUnit unit;
    /**
     * 延迟区间
     */
    private List<Integer> intervalList;
    /**
     * 延迟单位
     */
    private TimeUnit intervalUnit;
    /**
     * 执行耗时告警（毫秒）
     */
    private long executeWarn;
    /**
     * 执行区间
     */
    private List<Integer> executeList;
    /**
     * 执行单位
     */
    private TimeUnit executeUnit;
}
