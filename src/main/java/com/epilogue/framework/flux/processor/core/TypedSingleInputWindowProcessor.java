package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

public abstract class TypedSingleInputWindowProcessor<C extends Context, T> extends TypedSingleInputProcessor<C, T> {

    @Parameter
    private Integer window;

    @Override
    protected Flux<Object> process(C context, Flux<T> predecessor,
        ProcessorConfig processorConfig) {
        Flux<List<T>> windowed;
        if (window > 0) {
            windowed = predecessor.buffer(window);
        } else {
            windowed = predecessor.buffer();
        }
        return windowed.flatMapSequential(list -> window(context, list, processorConfig));
    }

    protected abstract Flux<Object> window(C context, List<T> windowPredecessor,
        ProcessorConfig processorConfig);
}
