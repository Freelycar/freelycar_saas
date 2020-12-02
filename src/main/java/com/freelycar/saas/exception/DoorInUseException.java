package com.freelycar.saas.exception;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-11-24
 * Time: 11:06
 */
public class DoorInUseException extends Exception{
    public DoorInUseException() {
        super();
    }

    public DoorInUseException(String message) {
        super(message);
    }

    public DoorInUseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DoorInUseException(Throwable cause) {
        super(cause);
    }

    protected DoorInUseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
