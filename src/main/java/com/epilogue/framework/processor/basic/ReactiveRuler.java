package com.epilogue.framework.processor.basic;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.BasicProcessor;
import com.epilogue.framework.processor.core.ProcessorConfig;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Mono;

public abstract class ReactiveRuler<C extends Context<T>, T> extends BasicProcessor<C> {

    /**
     * .
     * @param list 来自{@link Context#getItemList()}
     */
    protected abstract Mono<Object> reactiveRule(C context, Collection<T> list);

    @Override
    protected Mono<Object> process(C context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return reactiveRule(context, context.getItemList()).map(Function.identity());
    }
}
