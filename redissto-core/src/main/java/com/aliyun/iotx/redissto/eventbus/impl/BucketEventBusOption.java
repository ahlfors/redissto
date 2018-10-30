package com.aliyun.iotx.redissto.eventbus.impl;

import java.util.HashMap;
import java.util.Map;

import com.aliyun.iotx.redissto.eventbus.EventHandler;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jiehong.jh
 * @date 2018/10/16
 */
@Getter
@Setter
public class BucketEventBusOption {
    /**
     * 精度（ms）
     */
    private int precision = 10;
    /**
     * 偏差（ms）> 精度（ms）
     */
    private int deviation = 20;
    /**
     * 调度线程数
     */
    private int coreThreads = 16;
    /**
     * 集群模式下的 Topic 预定配置
     */
    private Map<String, EventHandler<?>> clusterTopicConfig = new HashMap<>();
}
