package com.freelycar.saas.exception;

public class BatchDeleteException extends Exception{
    public BatchDeleteException() {
    }

    public BatchDeleteException(String message) {
        super(message);
    }

    public BatchDeleteException(String message, Throwable cause) {
        super(message, cause);
    }

    public BatchDeleteException(Throwable cause) {
        super(cause);
    }

    public BatchDeleteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
