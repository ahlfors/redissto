package com.aliyun.iotx.redissto.storage;

import java.util.Iterator;

/**
 * @author jiehong.jh
 * @date 2018/9/11
 */
public interface StorageIterator extends Iterator<StorageProperty>, AutoCloseable {

    /**
     * auto close
     */
    @Override
    void close();
}
