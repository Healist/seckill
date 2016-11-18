package com.healist.seckill.exception;

/**
 * Created by Healist on 2016/11/16.
 */
public class SeckillException extends RuntimeException {
    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
