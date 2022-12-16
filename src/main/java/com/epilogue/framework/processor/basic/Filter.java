package com.epilogue.framework.processor.basic;

import com.epilogue.framework.processor.common.Context;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class Filter<C extends Context<T>, T> extends ReactiveFilter<C, T> {

    /**
     * .
     * @param list 来自{@link Context#getItemList()}
     */
    protected abstract Collection<T> filter(C context, Collection<T> list);

    @Override
    protected Mono<Collection<T>> reactiveFilter(C context, Collection<T> list) {
        try {
            return Mono.just(filter(context, context.getItemList()));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
