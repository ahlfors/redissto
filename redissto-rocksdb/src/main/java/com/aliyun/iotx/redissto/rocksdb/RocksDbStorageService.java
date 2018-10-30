package com.aliyun.iotx.redissto.rocksdb;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.aliyun.iotx.redissto.storage.StorageIterator;
import com.aliyun.iotx.redissto.storage.StorageKey;
import com.aliyun.iotx.redissto.storage.StorageProperty;
import com.aliyun.iotx.redissto.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

/**
 * @author jiehong.jh
 * @date 2018/8/13
 */
@Slf4j
public class RocksDbStorageService implements StorageService {

    private static final String FILE_PATH = "RocksDB";

    /**
     * Max number of in-memory write buffers (memtables) for the cache (tryReconnect and immutable).
     */
    private static final int MAX_WRITE_BUFFER_NUMBER = 1 << 2;

    /**
     * Minimum number of in-memory write buffers (memtables) to be merged before flushing to storage.
     */
    private static final int MIN_WRITE_BUFFER_NUMBER_TO_MERGE = 2;

    static {
        RocksDB.loadLibrary();
        log.info("Load the RocksDB shared library success.");
    }

    private final AtomicBoolean closed = new AtomicBoolean();
    private final RocksDB database;
    private Options databaseOptions;
    private WriteOptions writeOptions;

    /**
     * Creates a new instance of the RocksDBCache class.
     *
     * @param config RocksDB configuration.
     */
    public RocksDbStorageService(RocksDbConfig config) {
        String dbDir = Paths.get(config.getDatabaseDir(), FILE_PATH).toString();
        File dir = new File(dbDir);
        if (dir.mkdirs()) {
            log.info("Created empty database directory '{}'.", dir.getAbsolutePath());
        } else {
            log.info("Existed database directory '{}'.", dir.getAbsolutePath());
        }
        try {
            BlockBasedTableConfig tableFormatConfig = new BlockBasedTableConfig()
                .setBlockSize(config.getCacheBlockSizeKB() << 10)
                .setBlockCacheSize(config.getReadCacheSizeMB() << 20)
                .setCacheIndexAndFilterBlocks(true);
            this.databaseOptions = new Options()
                .setCreateIfMissing(true)
                .setWriteBufferSize(config.getWriteBufferSizeMB() << 18)
                .setMaxWriteBufferNumber(MAX_WRITE_BUFFER_NUMBER)
                .setMinWriteBufferNumberToMerge(MIN_WRITE_BUFFER_NUMBER_TO_MERGE)
                .setTableFormatConfig(tableFormatConfig)
                .setOptimizeFiltersForHits(true)
                .setUseDirectReads(true);
            this.writeOptions = new WriteOptions();
            this.database = RocksDB.open(databaseOptions, dbDir);
            log.info("Initialize RocksDB instance completed.");
        } catch (Exception ex) {
            try {
                close();
            } catch (Exception closeEx) {
                log.error("Close RocksDB error", closeEx);
                ex.addSuppressed(closeEx);
            }
            throw new RuntimeException("Initialize RocksDB instance", ex);
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (database != null) {
                database.close();
            }
            if (writeOptions != null) {
                writeOptions.close();
            }
            if (databaseOptions != null) {
                databaseOptions.close();
            }
            log.info("RocksDB Closed.");
        }
    }

    @Override
    public void insert(StorageKey key, byte[] data) {
        checkRocksDbState();
        try {
            database.put(writeOptions, key.serialize(), data);
        } catch (RocksDBException ex) {
            throw convert(ex, "insert key '%s'", key);
        }
    }

    @Override
    public void batchInsert(List<StorageProperty> recordList) {
        checkRocksDbState();
        try {
            WriteBatch writeBatch = new WriteBatch();
            for (StorageProperty record : recordList) {
                writeBatch.put(record.getKey().serialize(), record.getValue());
            }
            database.write(writeOptions, writeBatch);
        } catch (RocksDBException ex) {
            List<StorageKey> keyList = recordList.stream()
                .map(StorageProperty::getKey)
                .collect(Collectors.toList());
            throw convert(ex, "batch insert keys '%s'", keyList);
        }
    }

    @Override
    public byte[] get(StorageKey key) {
        checkRocksDbState();
        try {
            return database.get(key.serialize());
        } catch (RocksDBException ex) {
            throw convert(ex, "get key '%s'", key);
        }
    }

    @Override
    public StorageIterator iterator() {
        checkRocksDbState();
        return new RocksDbIterator(database.newIterator());
    }

    @Override
    public void remove(StorageKey key) {
        checkRocksDbState();
        try {
            database.delete(writeOptions, key.serialize());
        } catch (RocksDBException ex) {
            throw convert(ex, "remove key '%s'", key);
        }
    }

    @Override
    public void batchRemove(List<StorageKey> keys) {
        checkRocksDbState();
        try {
            WriteBatch writeBatch = new WriteBatch();
            for (StorageKey key : keys) {
                writeBatch.delete(key.serialize());
            }
            database.write(writeOptions, writeBatch);
        } catch (RocksDBException ex) {
            throw convert(ex, "batch remove keys '%s'", keys);
        }
    }

    private RuntimeException convert(RocksDBException exception, String format, Object... args) {
        String message = String.format("Unable to %s.", String.format(format, args));
        throw new RuntimeException(message, exception);
    }

    private void checkRocksDbState() {
        if (closed.get()) {
            throw new IllegalStateException("RocksDB is closed");
        }
        if (database == null) {
            throw new IllegalStateException("RocksDB has not been initialized.");
        }
    }
}
