package com.aliyun.iotx.redissto.rocksdb;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jiehong.jh
 * @date 2018/8/13
 */
@Getter
@Setter
public class RocksDbConfig {

    /**
     * The path to the RocksDB database (in the local filesystem).
     */
    private String databaseDir = "/home/admin";

    /**
     * RocksDB allows to buffer writes in-memory (memtables) to improve write performance, thus executing an async flush
     * process of writes to disk. This parameter bounds the maximum amount of memory devoted to absorb writes.
     */
    private int writeBufferSizeMB = 64;

    /**
     * RocksDB caches (uncompressed) data blocks in memory to serve read requests with high performance in case of a
     * cache hit. This parameter bounds the maximum amount of memory devoted to cache uncompressed data blocks.
     */
    private int readCacheSizeMB = 8;

    /**
     * RocksDB stores data in memory related to internal indexes (e.g., it may range between 5% to 30% of the total
     * memory consumption depending on the configuration and data at hand). The size of the internal indexes in RocksDB
     * mainly depend on the size of cached data blocks (cacheBlockSizeKB). If you increase cacheBlockSizeKB, the number
     * of blocks will decrease, so the index size will also reduce linearly (but increasing read amplification).
     */
    private int cacheBlockSizeKB = 32;

}
