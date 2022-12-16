package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

public abstract class TypedSingleInputProcessor<C extends Context, T> extends TypedBasicProcessor<C, T> {

    @Override
    protected final Flux<Object> process(C context, List<Flux<T>> predecessors,
        ProcessorConfig processorConfig) {
        return process(context, predecessors.get(0), processorConfig);
    }

    protected abstract Flux<Object> process(C context, Flux<T> predecessor,
        ProcessorConfig processorConfig);
}
