package com.aliyun.iotx.redissto.support;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author jiehong.jh
 * @date 2018/8/14
 */
public class HostAddressUtils {

    /**
     * local host address
     *
     * @return
     */
    public static String getCurrent() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("unknown host", e);
        }
    }
}
