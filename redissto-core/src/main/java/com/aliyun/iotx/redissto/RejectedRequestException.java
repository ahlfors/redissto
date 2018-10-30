package com.aliyun.iotx.redissto;

/**
 * request can not allow to access.
 *
 * @author jiehong.jh
 * @date 2018/10/29
 */
public class RejectedRequestException extends RuntimeException {

    private static final String MESSAGE = "Don't allow access to redis.";

    public RejectedRequestException() {
        super(MESSAGE);
    }

    public RejectedRequestException(String message) {
        super(message);
    }

    public RejectedRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public RejectedRequestException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
