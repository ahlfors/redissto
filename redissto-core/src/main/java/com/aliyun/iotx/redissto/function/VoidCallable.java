package com.aliyun.iotx.redissto.function;

import java.util.concurrent.Callable;

/**
 * @author jiehong.jh
 * @date 2018/8/3
 */
@FunctionalInterface
public interface VoidCallable extends Callable<Void> {

    /**
     * call
     *
     * @return
     * @throws Exception
     */
    @Override
    default Void call() throws Exception {
        run();
        return null;
    }

    /**
     * run without result
     *
     * @throws Exception
     */
    void run() throws Exception;
}
