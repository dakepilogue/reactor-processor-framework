package com.epilogue.framework.processor.core;

import lombok.Getter;

@Getter
public class Try<T> {

    private T returned;
    private Throwable throwable;

    public Try(T returned) {
        this.returned = returned;
    }

    public Try(Throwable throwable) {
        this.throwable = throwable;
    }

    public boolean isReturn() {
        return this.returned != null;
    }

    public boolean isThrow() {
        return this.throwable != null;
    }
}
