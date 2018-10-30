package com.aliyun.iotx.redissto.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jiehong.jh
 * @date 2018/8/14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StorageProperty {

    private StorageKey key;
    private byte[] value;
}
