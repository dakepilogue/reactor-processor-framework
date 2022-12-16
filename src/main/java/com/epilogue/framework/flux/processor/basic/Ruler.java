package com.epilogue.framework.flux.processor.basic;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

public abstract class Ruler<C extends Context<T>, T> extends ReactiveRuler<C, T> {

    @Parameter
    private int bufferSize;

    protected abstract List<T> rule(C context, List<T> list);

    @Override
    protected final Flux<T> reactiveRule(C context, Flux<T> flux) {
        try {
            return flux.buffer(bufferSize).flatMapIterable(list -> rule(context, list));
        } catch (Exception e) {
            return Flux.error(e);
        }
    }
}
