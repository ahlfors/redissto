package com.aliyun.iotx.redissto.tablestore;

import com.alicloud.openservices.tablestore.model.PrimaryKeyType;
import com.aliyun.iotx.fluentable.annotation.TableStorePrimaryKey;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author jiehong.jh
 * @date 2018/9/26
 */
@Getter
@Setter
@ToString
public class CommandBatch {
    @TableStorePrimaryKey(type = PrimaryKeyType.INTEGER)
    private Long id;
    /**
     * 批次个数
     */
    private Integer size;
    /**
     * 批次状态
     */
    private String status;
}
