package com.aliyun.iotx.redissto.leveldb;

import java.io.IOException;
import java.util.Map.Entry;

import com.aliyun.iotx.redissto.storage.StorageIterator;
import com.aliyun.iotx.redissto.storage.StorageKey;
import com.aliyun.iotx.redissto.storage.StorageProperty;
import org.iq80.leveldb.DBIterator;

/**
 * @author jiehong.jh
 * @date 2018/10/8
 */
public class LevelDbIterator implements StorageIterator {

    private volatile boolean closed;
    private DBIterator iterator;

    public LevelDbIterator(DBIterator iterator) {
        this.iterator = iterator;
        iterator.seekToFirst();
    }

    @Override
    public boolean hasNext() {
        if (this.closed) {
            throw new IllegalStateException("LevelDB is closed");
        }
        return iterator.hasNext();
    }

    @Override
    public StorageProperty next() {
        if (this.closed) {
            throw new IllegalStateException("LevelDB is closed");
        }
        Entry<byte[], byte[]> entry = iterator.peekNext();
        StorageProperty property = new StorageProperty();
        property.setKey(new StorageKey(entry.getKey()));
        property.setValue(entry.getValue());
        iterator.next();
        return property;
    }

    @Override
    public void close() {
        closed = true;
        try {
            iterator.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
