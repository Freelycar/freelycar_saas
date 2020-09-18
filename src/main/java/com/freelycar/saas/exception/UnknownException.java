package com.freelycar.saas.exception;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-17
 * Time: 18:24
 */
public class UnknownException extends Exception{
    public UnknownException() {
        super();
    }

    public UnknownException(String message) {
        super(message);
    }

    public UnknownException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownException(Throwable cause) {
        super(cause);
    }

    protected UnknownException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
