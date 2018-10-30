package com.aliyun.iotx.redissto;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jiehong.jh
 * @date 2018/8/2
 */
public class DualClient {

    @Autowired
    private RedisExecutor redisExecutor;
    private ExecutorService asyncPool;
    @Autowired(required = false)
    private DualMonitor monitor;

    public DualClient asyncPool(ExecutorService asyncPool) {
        this.asyncPool = asyncPool;
        return this;
    }

    /**
     * 获取 DualReference
     *
     * @param dualKey
     * @return
     */
    public DualReference opsFor(DualKey dualKey) {
        return new DualReference(dualKey.value());
    }

    /**
     * 发起 DualRequest
     *
     * @param reference
     * @param <T>
     * @return
     */
    public <T> DualRequest<T> newCall(DualReference reference) {
        return new DualRequest<>(reference);
    }

    public class DualReference {
        private final String key;

        private DualReference(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        RedisExecutor executor() {
            return redisExecutor;
        }

        ExecutorService asyncPool() {
            return asyncPool;
        }

        DualMonitor monitor() {
            return monitor;
        }
    }
}
