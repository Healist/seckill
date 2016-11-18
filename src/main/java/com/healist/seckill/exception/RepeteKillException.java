package com.healist.seckill.exception;

/**
 * Created by Healist on 2016/11/16.
 */
public class RepeteKillException extends SeckillException {

    public RepeteKillException(String message) {
        super(message);
    }

    public RepeteKillException(String message, Throwable cause) {
        super(message, cause);
    }

}
