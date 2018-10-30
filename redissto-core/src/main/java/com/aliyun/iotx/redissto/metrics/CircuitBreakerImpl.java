package com.aliyun.iotx.redissto.metrics;

import java.util.concurrent.TimeUnit;

import com.aliyun.iotx.redissto.support.PureRateLimiter;
import com.aliyun.iotx.redissto.support.QpsRateLimiter;
import com.aliyun.iotx.redissto.support.RateLimiter;
import com.aliyun.iotx.redissto.support.SlideWindow;
import com.aliyun.iotx.redissto.support.TimerSlideWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jiehong.jh
 * @date 2018/8/1
 */
public class CircuitBreakerImpl implements CircuitBreaker {

    private static final int SLIDE_WINDOW_LENGTH = 5;
    private static Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
    private final SlideWindow<Boolean> slideWindow;
    /**
     * number of requests allowed when it is half-open.
     */
    private final RateLimiter rateLimiter;
    private CircuitBreakerStatus status;

    /**
     * @param properties
     */
    public CircuitBreakerImpl(MetricProperties properties) {
        long time = properties.getTime();
        TimeUnit unit = properties.getUnit();
        int warnThreshold = properties.getWarnThreshold();
        int errorThreshold = properties.getErrorThreshold();
        status = new CircuitBreakerStatus(warnThreshold, errorThreshold);
        MetricBucket bucket = new MetricBucket(status, properties.getOkPermits());
        slideWindow = new TimerSlideWindow<>(SLIDE_WINDOW_LENGTH, bucket, time, unit);
        int permits = properties.getPermits();
        if (permits == 1) {
            rateLimiter = new PureRateLimiter(time, unit);
        } else {
            rateLimiter = new QpsRateLimiter(time, unit, permits);
        }
    }

    @Override
    public void close() throws Exception {
        slideWindow.close();
    }

    @Override
    public boolean allowRequest() {
        if (status.nativeState == CircuitBreakerStatus.CLOSE) {
            return true;
        }
        if (status.nativeState == CircuitBreakerStatus.HALF_OPEN) {
            return rateLimiter.tryAcquire();
        }
        return false;
    }

    @Override
    public boolean isClose() {
        return status.nativeState == CircuitBreakerStatus.CLOSE;
    }

    @Override
    public void markSuccess() {
        slideWindow.put(true);
    }

    @Override
    public void markNonSuccess() {
        slideWindow.put(false);
    }

    private class CircuitBreakerStatus implements MetricStatus {

        private static final int CLOSE = 0;
        private static final int HALF_OPEN = 1;
        private static final int OPEN = 2;
        /**
         * the native state
         * <p>
         */
        protected volatile int nativeState = CLOSE;

        /**
         * success ratio warn threshold
         */
        private int warnThreshold;
        /**
         * success ratio error threshold
         */
        private int errorThreshold;

        /**
         * 100 > warnThreshold > errorThreshold > 0
         *
         * @param warnThreshold
         * @param errorThreshold
         */
        public CircuitBreakerStatus(int warnThreshold, int errorThreshold) {
            this.warnThreshold = warnThreshold;
            this.errorThreshold = errorThreshold;
        }

        @Override
        public void setState(int ratio) {
            if (ratio > warnThreshold) {
                nativeState = CLOSE;
                logger.info("the native state is closed");
                return;
            }
            if (ratio > errorThreshold) {
                nativeState = HALF_OPEN;
                logger.warn("the native state is in a half-open state");
                return;
            }
            logger.warn("the native state is in a full-open state");
            nativeState = OPEN;
        }

        @Override
        public void setState() {
            if (nativeState == OPEN) {
                nativeState = HALF_OPEN;
                logger.warn("the native state is full-open to a half-open state");
            } else {
                logger.info("the native state keep {} state", nativeState == CLOSE ? "closed" : "half-open");
            }
        }

        @Override
        public boolean isHalfOpen() {
            return nativeState == HALF_OPEN;
        }

        @Override
        public void close() {
            nativeState = CLOSE;
            logger.info("the native state quickly close");
        }
    }
}
