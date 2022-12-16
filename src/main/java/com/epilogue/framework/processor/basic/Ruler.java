package com.epilogue.framework.processor.basic;

import com.epilogue.framework.processor.common.Context;
import java.util.Collection;
import reactor.core.publisher.Mono;

public abstract class Ruler<C extends Context<T>, T> extends ReactiveRuler<C, T> {

    protected abstract void rule(C context, Collection<T> list);

    @Override
    protected final Mono<Object> reactiveRule(C context, Collection<T> list) {
        try {
            rule(context, context.getItemList());
            return Mono.empty();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
