package com.aliyun.iotx.redissto.support;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @author jiehong.jh
 * @date 2018/8/3
 */
public class SleepUtils {

    /**
     * block thread
     *
     * @param time
     * @param unit
     */
    public static void block(long time, TimeUnit unit) {
        if (time <= 0) {
            return;
        }
        long timeout = unit.toNanos(time);
        final long end = System.nanoTime() + timeout;
        while (true) {
            try {
                NANOSECONDS.sleep(timeout);
                return;
            } catch (InterruptedException e) {
                timeout = end - System.nanoTime();
            }
        }
    }
}
