package com.aliyun.iotx.redissto.failover;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.aliyun.iotx.redissto.metrics.ClusterSynchronizer;
import com.aliyun.iotx.redissto.metrics.MetricBucket;
import com.aliyun.iotx.redissto.metrics.MetricProperties;
import com.aliyun.iotx.redissto.metrics.MetricStatus;
import com.aliyun.iotx.redissto.metrics.NativeWatchdog;
import com.aliyun.iotx.redissto.support.PureRateLimiter;
import com.aliyun.iotx.redissto.support.QpsRateLimiter;
import com.aliyun.iotx.redissto.support.RateLimiter;
import com.aliyun.iotx.redissto.support.SimpleThreadFactory;
import com.aliyun.iotx.redissto.support.SlideWindow;
import com.aliyun.iotx.redissto.support.TimerSlideWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jiehong.jh
 * @date 2018/8/16
 */
public abstract class AbstractClusterSynchronizer implements ClusterSynchronizer {

    private static final int SLIDE_WINDOW_LENGTH = 5;
    /**
     * ensure only one thread to restore local records
     */
    private final Executor executor = new ThreadPoolExecutor(1, 1, 60L,
        TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new SimpleThreadFactory("cluster-sentinel"));
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private SynchronizerStatus status;
    private long time;
    private TimeUnit unit;
    private int permits;
    private int okPermits;
    private NativeWatchdog watchdog;
    private SlideWindow<Boolean> slideWindow;
    /**
     * number of requests allowed when it is half-open.
     */
    private RateLimiter rateLimiter;

    /**
     * @param properties
     */
    public AbstractClusterSynchronizer(MetricProperties properties) {
        this.status = new SynchronizerStatus(properties.getWarnThreshold(), properties.getErrorThreshold());
        this.time = properties.getTime();
        this.unit = properties.getUnit();
        this.permits = properties.getPermits();
        this.okPermits = properties.getOkPermits();
    }

    public void setWatchdog(NativeWatchdog watchdog) {
        this.watchdog = watchdog;
    }

    @Override
    public void start() {
        if (watchdog == null) {
            throw new NullPointerException("watchdog is null");
        }
        MetricBucket bucket = new MetricBucket(status, okPermits);
        this.slideWindow = new TimerSlideWindow<>(SLIDE_WINDOW_LENGTH, bucket, time, unit);
        if (permits == 1) {
            this.rateLimiter = new PureRateLimiter(time, unit);
        } else {
            this.rateLimiter = new QpsRateLimiter(time, unit, permits);
        }
        initialize();
    }

    @Override
    public void close() throws Exception {
        slideWindow.close();
    }

    /**
     * Perform initialization after the storage synchronizer start
     */
    protected void initialize() {
    }

    @Override
    public boolean allowRead() {
        // CLOSE or HALF_OPEN
        return status.clusterState && (status.isClose() || (status.isHalfOpen() && rateLimiter.tryAcquire()));
    }

    @Override
    public boolean allowWrite() {
        // CLOSE or HALF_OPEN or WAIT_CLOSE
        if (status.isOpen()) {
            return false;
        }
        if (status.isHalfOpen()) {
            return rateLimiter.tryAcquire();
        }
        return true;
    }

    @Override
    public void markSuccess() {
        slideWindow.put(true);
    }

    @Override
    public void markNonSuccess() {
        slideWindow.put(false);
    }

    /**
     * Get the native state to determine if the native state is consistent with the remote state
     *
     * @return
     */
    protected boolean nativeState() {
        return status.isClose() || status.isHalfOpen();
    }

    protected void markClusterState(boolean clusterState) {
        logger.info("the cluster state is {}", clusterState);
        status.clusterState = clusterState;
    }

    private class SynchronizerStatus implements MetricStatus {

        private static final int CLOSE = 0;
        private static final int HALF_OPEN = 1;
        private static final int OPEN = 2;
        private static final int WAIT_CLOSE = 3;

        /**
         * the native state
         */
        protected volatile AtomicInteger nativeState = new AtomicInteger(CLOSE);
        /**
         * the cluster state
         */
        private volatile boolean clusterState = true;

        /**
         * success ratio warn threshold
         */
        private int warnThreshold;
        /**
         * success ratio error threshold
         */
        private int errorThreshold;

        private SynchronizerStatus(int warnThreshold, int errorThreshold) {
            this.warnThreshold = warnThreshold;
            this.errorThreshold = errorThreshold;
        }

        @Override
        public void setState(int ratio) {
            if (nativeState.get() >= OPEN) {
                startWatchdog();
                return;
            }
            executor.execute(() -> watchdog.doRecovery());
            if (ratio > warnThreshold) {
                nativeState.set(CLOSE);
                logger.info("the native state is closed");
                return;
            }
            if (ratio > errorThreshold) {
                nativeState.set(HALF_OPEN);
                logger.warn("the native state is in a half-open state");
                return;
            }
            logger.warn("the native state is in a full-open state and the cluster state is false");
            nativeState.set(OPEN);
            clusterState = false;
            broadcast(false);
        }

        @Override
        public void setState() {
            if (nativeState.get() >= OPEN) {
                startWatchdog();
            } else {
                executor.execute(() -> watchdog.doRecovery());
                logger.info("the native state keep {} state", nativeState.get() == CLOSE ? "closed" : "half-open");
            }
        }

        private boolean isOpen() {
            return nativeState.get() == OPEN;
        }

        @Override
        public boolean isHalfOpen() {
            return nativeState.get() == HALF_OPEN;
        }

        private boolean isClose() {
            return nativeState.get() == CLOSE;
        }

        @Override
        public void close() {
            nativeState.set(CLOSE);
            logger.info("the native state quickly close");
        }

        private void startWatchdog() {
            try {
                if (watchdog.tryReconnect()) {
                    logger.info("watchdog connect success.");
                    if (clusterState) {
                        logger.info("the cluster state is true");
                        close();
                        return;
                    }
                    if (nativeState.compareAndSet(OPEN, WAIT_CLOSE)) {
                        executor.execute(() -> {
                            if (watchdog.doRecovery() && nativeState.compareAndSet(WAIT_CLOSE, CLOSE)) {
                                logger.info("the native state from open to close");
                                broadcast(true);
                            }
                        });
                    }
                } else {
                    logger.warn("watchdog connect failure.");
                    nativeState.set(OPEN);
                }
                logger.info("the cluster state is false");
            } catch (Exception e) {
                logger.error("watchdog connect error.", e);
            }
        }
    }
}
