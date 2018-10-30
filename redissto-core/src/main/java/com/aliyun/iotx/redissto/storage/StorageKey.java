package com.aliyun.iotx.redissto.storage;

import java.util.Arrays;

/**
 * @author jiehong.jh
 * @date 2018/8/14
 */
public class StorageKey {

    private byte[] value;

    public StorageKey(String value) {
        this.value = value.getBytes();
    }

    public StorageKey(byte[] bytes) {
        this.value = bytes;
    }

    public byte[] serialize() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageKey that = (StorageKey)o;
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return new String(value);
    }
}
