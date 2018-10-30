package com.aliyun.iotx.redissto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aliyun.iotx.redissto.DualClient.DualReference;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

/**
 * @author jiehong.jh
 * @date 2018/10/9
 */
@Slf4j
public class DualRequest<T> {

    private static final String NIL_KEY = "NIL:";
    private static final String NIL_VALUE = "@nil";

    private final String key;
    private ExecutorService asyncPool;
    private RedisExecutor executor;
    private RedissonClient client;
    private DualMonitor monitor;

    public DualRequest(DualReference reference) {
        this.key = reference.getKey();
        this.asyncPool = reference.asyncPool();
        this.executor = reference.executor();
        this.client = executor.getClient();
        this.monitor = reference.monitor();
    }

    /**
     * 如果缓存有值则返回，否则使用{@link Supplier#get}获取值
     *
     * @param other
     * @return
     */
    public Response getIfAbsent(Supplier<T> other) {
        RBucket<T> bucket = client.getBucket(key);
        return executor.syncGet(bucket::get)
            .handle((t, e) -> {
                if (e == null) {
                    if (t != null) {
                        return new Response(t, true);
                    }
                    RBucket<String> nilBucket = client.getBucket(NIL_KEY + key);
                    CompletableFuture<String> nilFuture = executor.syncGet(nilBucket::get);
                    if (!nilFuture.isCompletedExceptionally() && NIL_VALUE.equals(nilFuture.join())) {
                        return new Response(null, true);
                    }
                }
                T v = other.get();
                return new Response(v, false);
            }).join();
    }

    /**
     * 在 <code>runnable</code> 执行后设置缓存
     *
     * @param value
     * @param runnable
     */
    public void put(T value, Runnable runnable) {
        CompletableFuture.runAsync(inMonitor(runnable), asyncPool)
            .whenComplete((empty, throwable) -> {
                if (throwable != null) {
                    log.error("put: {}", key, throwable);
                } else {
                    RBucket<T> bucket = client.getBucket(key);
                    executor.set(() -> bucket.set(value, 30, TimeUnit.MINUTES));
                }
                executor.asyncDelete(NIL_KEY + key);
            });
    }

    /**
     * 在 <code>runnable</code> 执行后删除缓存
     *
     * @param runnable
     */
    public void delete(Runnable runnable) {
        CompletableFuture.runAsync(inMonitor(runnable), asyncPool)
            .whenComplete((empty, throwable) -> {
                if (throwable != null) {
                    log.error("delete: {}", key, throwable);
                }
                executor.asyncDelete(key, NIL_KEY + key);
            });
    }

    private Runnable inMonitor(Runnable runnable) {
        if (monitor == null || monitor.isClose()) {
            return runnable;
        }
        final long start = System.currentTimeMillis();
        return () -> {
            long current = System.currentTimeMillis();
            monitor.observeInterval(current - start);
            runnable.run();
            monitor.observeExecute(System.currentTimeMillis() - current);
        };
    }

    /**
     * 设置缓存
     *
     * @param value
     */
    public void put(T value) {
        RBucket<T> bucket = client.getBucket(key);
        executor.set(() -> bucket.set(value, 30, TimeUnit.MINUTES));
        executor.asyncDelete(NIL_KEY + key);
    }

    /**
     * 删除缓存
     */
    public void delete() {
        executor.asyncDelete(key, NIL_KEY + key);
    }

    public class Response {

        private T value;
        private boolean fromCache;
        private boolean execCache;

        private Response(T value, boolean fromCache) {
            this.value = value;
            this.fromCache = fromCache;
        }

        /**
         * 是否将获取到的值缓存
         *
         * @return
         */
        public Response cache() {
            execCache = true;
            return this;
        }

        /**
         * 获取结果
         *
         * @return
         */
        public T get() {
            if (!fromCache && execCache) {
                RBucket<T> bucket = client.getBucket(key);
                executor.asyncSet(() -> bucket.setAsync(value, 30, TimeUnit.MINUTES));
                if (value == null) {
                    executor.asyncSet(() -> {
                        RBucket<String> nilBucket = client.getBucket(NIL_KEY + key);
                        return nilBucket.setAsync(NIL_VALUE, 30, TimeUnit.MINUTES);
                    });
                }
            }
            return value;
        }

    }
}
