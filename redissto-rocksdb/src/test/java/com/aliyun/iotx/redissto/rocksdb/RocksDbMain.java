package com.aliyun.iotx.redissto.rocksdb;

import java.util.ArrayList;
import java.util.List;

import com.aliyun.iotx.redissto.storage.StorageIterator;
import com.aliyun.iotx.redissto.storage.StorageKey;
import com.aliyun.iotx.redissto.storage.StorageProperty;
import com.aliyun.iotx.redissto.storage.StorageService;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * @author jiehong.jh
 * @date 2018/8/14
 */
public class RocksDbMain {

    public static void main(String[] args) {
        RocksDbConfig rocksDbConfig = new RocksDbConfig();
        rocksDbConfig.setDatabaseDir("/Users/lemonguge/");
        StorageService storageService = new RocksDbStorageService(rocksDbConfig);
        cache(storageService);

        //operate(storageService);
    }

    private static void operate(StorageService storageService) {
        for (int i = 0; i < 10; i++) {
            StorageKey storageKey = new StorageKey("key" + i);
            String randomAlphanumeric = RandomStringUtils.randomAlphanumeric(16);
            System.out.println(storageKey + "=" + randomAlphanumeric);
            storageService.insert(storageKey, randomAlphanumeric.getBytes());
        }

        System.out.println(new String(storageService.get(new StorageKey("key1"))));

        StorageIterator iterator = storageService.iterator();
        while (iterator.hasNext()) {
            StorageProperty property = iterator.next();
            System.out.println("key=" + property.getKey() + ", value=" + new String(property.getValue()));
        }

        System.out.println("---------------");
        List<StorageProperty> recordList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            StorageKey storageKey = new StorageKey("key1" + i);
            String randomAlphanumeric = RandomStringUtils.randomAlphanumeric(16);
            recordList.add(new StorageProperty(storageKey, randomAlphanumeric.getBytes()));
        }
        storageService.batchInsert(recordList);

        iterator = storageService.iterator();
        while (iterator.hasNext()) {
            StorageProperty property = iterator.next();
            System.out.println("key=" + property.getKey() + ", value=" + new String(property.getValue()));
        }

        System.out.println("---------------");
        List<StorageKey> keys = new ArrayList<>();
        keys.add(new StorageKey("key0"));
        keys.add(new StorageKey("key3"));
        keys.add(new StorageKey("key12"));
        keys.add(new StorageKey("key17"));
        storageService.batchRemove(keys);

        iterator = storageService.iterator();
        while (iterator.hasNext()) {
            StorageProperty property = iterator.next();
            System.out.println("key=" + property.getKey() + ", value=" + new String(property.getValue()));
        }

        System.out.println("---------------");
        iterator = storageService.iterator();
        while (iterator.hasNext()) {
            StorageProperty property = iterator.next();
            System.out.println("key=" + property.getKey() + ", value=" + new String(property.getValue()));
        }
    }

    private static void cache(StorageService storageService) {
        // key 不存在时，value is null
        StorageKey key = new StorageKey("hello");
        byte[] bytes = storageService.get(key);
        System.out.println(bytes == null);
        //System.out.println(new String(bytes)); // NPL
        storageService.insert(key, "world".getBytes());
        bytes = storageService.get(key);
        System.out.println(new String(bytes));

        storageService.insert(key, "new world".getBytes());
        bytes = storageService.get(key);
        System.out.println(new String(bytes));

        storageService.remove(key);
        bytes = storageService.get(key);
        System.out.println(bytes == null);
    }
}
