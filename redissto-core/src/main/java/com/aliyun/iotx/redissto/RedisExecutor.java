package com.aliyun.iotx.redissto;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import com.aliyun.iotx.redissto.failover.NativeFailoverSynchronizer;
import com.aliyun.iotx.redissto.function.AsyncCallable;
import com.aliyun.iotx.redissto.function.VoidCallable;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jiehong.jh
 * @date 2018/8/3
 */
@Slf4j
public class RedisExecutor {

    @Autowired
    private RedissonClient redissonClient;

    private NativeFailoverSynchronizer synchronizer;

    public RedisExecutor(NativeFailoverSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public RedissonClient getClient() {
        return redissonClient;
    }

    public NativeFailoverSynchronizer getSynchronizer() {
        return synchronizer;
    }

    /**
     * Ping all Redis nodes
     *
     * @return <code>true</code> if all nodes have replied "PONG", <code>false</code> in other case.
     */
    public boolean ping() {
        return redissonClient.getNodesGroup().pingAll();
    }

    public CompletableFuture<Void> get(VoidCallable command) {
        return execute(command, true);
    }

    public CompletableFuture<Void> set(VoidCallable command) {
        return execute(command, false);
    }

    private CompletableFuture<Void> execute(VoidCallable command, boolean isRead) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            if (isRead ? synchronizer.allowRead() : synchronizer.allowWrite()) {
                command.call();
                future.complete(null);
                synchronizer.markSuccess();
            } else {
                log.warn("the cluster synchronizer reject request {}.", (isRead ? "read" : "write"));
                future.completeExceptionally(new RejectedRequestException());
            }
        } catch (Throwable ex) {
            log.error("{} redis error.", (isRead ? "read" : "write"), ex);
            synchronizer.markNonSuccess();
            future.completeExceptionally(ex);
        }
        return future;
    }

    public <V> CompletableFuture<V> syncGet(Callable<V> command) {
        return syncExecute(command, true);
    }

    public <V> CompletableFuture<V> syncSet(Callable<V> command) {
        return syncExecute(command, false);
    }

    private <V> CompletableFuture<V> syncExecute(Callable<V> command, boolean isRead) {
        CompletableFuture<V> future = new CompletableFuture<>();
        try {
            if (isRead ? synchronizer.allowRead() : synchronizer.allowWrite()) {
                future.complete(command.call());
                synchronizer.markSuccess();
            } else {
                log.warn("the cluster synchronizer reject request {}.", (isRead ? "read" : "write"));
                future.completeExceptionally(new RejectedRequestException());
            }
        } catch (Throwable ex) {
            log.error("{} redis error.", (isRead ? "read" : "write"), ex);
            synchronizer.markNonSuccess();
            future.completeExceptionally(ex);
        }
        return future;
    }

    public <V> CompletableFuture<V> asyncGet(AsyncCallable<V> command) {
        return asyncExecute(command, true);
    }

    public <V> CompletableFuture<V> asyncSet(AsyncCallable<V> command) {
        return asyncExecute(command, false);
    }

    private <V> CompletableFuture<V> asyncExecute(AsyncCallable<V> command, boolean isRead) {
        if (isRead ? !synchronizer.allowRead() : !synchronizer.allowWrite()) {
            log.warn("the cluster synchronizer reject request {}.", (isRead ? "read" : "write"));
            CompletableFuture<V> future = new CompletableFuture<>();
            future.completeExceptionally(new RejectedRequestException());
            return future;
        }
        RFuture<V> rFuture = command.call();
        return rFuture.whenComplete((result, exception) -> {
            if (exception != null) {
                synchronizer.markNonSuccess();
                log.error("{} redis error.", (isRead ? "read" : "write"), exception);
            } else {
                synchronizer.markSuccess();
            }
        }).toCompletableFuture();
    }

    public CompletableFuture<Long> asyncDelete(final String... keys) {
        if (!synchronizer.allowWrite()) {
            log.warn("the cluster synchronizer reject request write.");
            synchronizer.failover(keys);
            CompletableFuture<Long> future = new CompletableFuture<>();
            future.completeExceptionally(new RejectedRequestException());
            return future;
        }
        RFuture<Long> future = redissonClient.getKeys().deleteAsync(keys);
        return future.whenComplete((result, exception) -> {
            if (exception != null) {
                synchronizer.markNonSuccess();
                log.error("write redis error.", exception);
                synchronizer.failover(keys);
            } else {
                synchronizer.markSuccess();
            }
        }).toCompletableFuture();
    }
}
