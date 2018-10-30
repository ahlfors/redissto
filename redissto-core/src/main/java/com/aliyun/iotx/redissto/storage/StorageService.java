package com.aliyun.iotx.redissto.storage;

import java.util.List;

/**
 * @author jiehong.jh
 * @date 2018/8/13
 */
public interface StorageService extends AutoCloseable {

    /**
     * Inserts a new record into the local DB.
     *
     * @param key  The the key of the entry.
     * @param data The payload associated with the given key.
     */
    void insert(StorageKey key, byte[] data);

    /**
     * Batch inserts new record list into the local DB.
     *
     * @param recordList
     */
    void batchInsert(List<StorageProperty> recordList);

    /**
     * Retrieves a record with given key.
     *
     * @param key The key to search by.
     * @return The payload associated with the key, or null if no such entry exists.
     */
    byte[] get(StorageKey key);

    /**
     * Retrieves record iterator.
     *
     * @return
     */
    StorageIterator iterator();

    /**
     * Removes any record that is associated with the given key.
     *
     * @param key The key of the entry to remove.
     */
    void remove(StorageKey key);

    /**
     * Removes any record that is associated with the given keys.
     *
     * @param keys
     */
    void batchRemove(List<StorageKey> keys);

    /**
     * Closes this local DB and releases all resources owned by it.
     */
    @Override
    void close();
}
