package com.aliyun.iotx.redissto.leveldb;

import lombok.Getter;
import lombok.Setter;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteOptions;

/**
 * @author jiehong.jh
 * @date 2018/10/8
 */
@Getter
@Setter
public class LevelDbConfig {

    /**
     * The path to the LevelDB database (in the local filesystem).
     */
    private String databaseDir = "/home/admin";

    /**
     * LevelDB options
     */
    private Options options = new Options();

    /**
     * LevelDB write options
     */
    private WriteOptions writeOptions = new WriteOptions();
}
