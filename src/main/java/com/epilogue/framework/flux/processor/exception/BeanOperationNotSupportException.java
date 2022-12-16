package com.epilogue.framework.flux.processor.exception;

public class BeanOperationNotSupportException extends RuntimeException {

    public BeanOperationNotSupportException() {
    }

    public BeanOperationNotSupportException(String message) {
        super(message);
    }

    public BeanOperationNotSupportException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanOperationNotSupportException(Throwable cause) {
        super(cause);
    }
}
