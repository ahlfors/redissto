package com.aliyun.iotx.redissto.rocksdb;

import com.aliyun.iotx.redissto.storage.StorageIterator;
import com.aliyun.iotx.redissto.storage.StorageKey;
import com.aliyun.iotx.redissto.storage.StorageProperty;
import org.rocksdb.RocksIterator;

/**
 * @author jiehong.jh
 * @date 2018/9/11
 */
public class RocksDbIterator implements StorageIterator {

    private volatile boolean closed;
    private RocksIterator rocksIterator;

    public RocksDbIterator(RocksIterator rocksIterator) {
        this.rocksIterator = rocksIterator;
        rocksIterator.seekToFirst();
    }

    @Override
    public boolean hasNext() {
        if (this.closed) {
            throw new IllegalStateException("RocksDB is closed");
        }
        return rocksIterator.isValid();
    }

    @Override
    public StorageProperty next() {
        if (this.closed) {
            throw new IllegalStateException("RocksDB is closed");
        }
        StorageProperty property = new StorageProperty();
        property.setKey(new StorageKey(rocksIterator.key()));
        property.setValue(rocksIterator.value());
        rocksIterator.next();
        return property;
    }

    @Override
    public void close() {
        closed = true;
        rocksIterator.close();
    }
}
