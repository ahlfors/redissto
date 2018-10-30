package com.aliyun.iotx.redissto.tablestore;

import com.alicloud.openservices.tablestore.model.PrimaryKey;
import com.aliyun.iotx.redissto.DualKey;

/**
 * 表格存储双写Key
 *
 * @author jiehong.jh
 * @date 2018/10/26
 */
public class TableStoreDualKey implements DualKey {

    private String value;

    public TableStoreDualKey(String tableName, PrimaryKey primaryKey) {
        StringBuilder builder = new StringBuilder(tableName);
        int size = primaryKey.size();
        for (int i = 0; i < size; i++) {
            builder.append(":").append(primaryKey.getPrimaryKeyColumn(i).getValue().toString());
        }
        this.value = builder.toString();
    }

    @Override
    public String value() {
        return value;
    }
}
