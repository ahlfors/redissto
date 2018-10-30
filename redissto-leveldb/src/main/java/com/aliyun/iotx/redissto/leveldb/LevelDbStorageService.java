package com.aliyun.iotx.redissto.leveldb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aliyun.iotx.redissto.storage.StorageIterator;
import com.aliyun.iotx.redissto.storage.StorageKey;
import com.aliyun.iotx.redissto.storage.StorageProperty;
import com.aliyun.iotx.redissto.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.iq80.leveldb.impl.Iq80DBFactory;

/**
 * @author jiehong.jh
 * @date 2018/10/8
 */
@Slf4j
public class LevelDbStorageService implements StorageService {

    private static final String FILE_PATH = "LevelDB";

    private final AtomicBoolean closed = new AtomicBoolean();
    private final DB database;
    private WriteOptions writeOptions;

    public LevelDbStorageService(LevelDbConfig config) {
        String dbDir = Paths.get(config.getDatabaseDir(), FILE_PATH).toString();
        File dir = new File(dbDir);
        if (dir.mkdirs()) {
            log.info("Created empty database directory '{}'.", dir.getAbsolutePath());
        } else {
            log.info("Existed database directory '{}'.", dir.getAbsolutePath());
        }
        try {
            this.writeOptions = config.getWriteOptions();
            this.database = Iq80DBFactory.factory.open(dir, config.getOptions());
            log.info("Initialize LevelDB instance completed.");
        } catch (Exception ex) {
            close();
            throw new RuntimeException("Initialize LevelDB instance", ex);
        }
    }

    @Override
    public void insert(StorageKey key, byte[] data) {
        checkLevelDbState();
        database.put(key.serialize(), data);
    }

    @Override
    public void batchInsert(List<StorageProperty> recordList) {
        checkLevelDbState();
        WriteBatch writeBatch = database.createWriteBatch();
        try {
            for (StorageProperty record : recordList) {
                writeBatch.put(record.getKey().serialize(), record.getValue());
            }
            database.write(writeBatch, writeOptions);
        } finally {
            try {
                writeBatch.close();
            } catch (IOException e) {
                log.error("LevelDB write batch close failure", e);
            }
        }
    }

    @Override
    public byte[] get(StorageKey key) {
        checkLevelDbState();
        return database.get(key.serialize());
    }

    @Override
    public StorageIterator iterator() {
        checkLevelDbState();
        return new LevelDbIterator(database.iterator());
    }

    @Override
    public void remove(StorageKey key) {
        checkLevelDbState();
        database.delete(key.serialize(), writeOptions);
    }

    @Override
    public void batchRemove(List<StorageKey> keys) {
        checkLevelDbState();
        WriteBatch writeBatch = database.createWriteBatch();
        for (StorageKey key : keys) {
            writeBatch.delete(key.serialize());
        }
        database.write(writeBatch, writeOptions);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (database != null) {
                try {
                    database.close();
                } catch (Exception e) {
                    log.error("LevelDB close failure", e);
                }
            }
            log.info("LevelDB Closed.");
        }
    }

    private void checkLevelDbState() {
        if (closed.get()) {
            throw new IllegalStateException("LevelDB is closed");
        }
        if (database == null) {
            throw new IllegalStateException("LevelDB has not been initialized.");
        }
    }
}
