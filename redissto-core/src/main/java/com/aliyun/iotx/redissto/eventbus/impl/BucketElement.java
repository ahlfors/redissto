package com.aliyun.iotx.redissto.eventbus.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jiehong.jh
 * @date 2018/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BucketElement {
    private String topic;
    private Object message;
    private boolean cluster;
}
