package com.epilogue.framework.flux.processor.basic;

import com.epilogue.framework.flux.processor.common.Context;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class Filter<C extends Context<T>, T> extends ReactiveFilter<C, T> {

    /**
     * .
     */
    protected abstract boolean filter(C context, T item);

    @Override
    protected Mono<T> reactiveFilter(C context, T item) {
        try {
            if (filter(context, item)) {
                return Mono.just(item);
            } else {
                return Mono.empty();
            }
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
