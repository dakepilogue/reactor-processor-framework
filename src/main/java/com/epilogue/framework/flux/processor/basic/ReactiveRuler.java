package com.epilogue.framework.flux.processor.basic;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.BasicProcessor;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Flux;

public abstract class ReactiveRuler<C extends Context<T>, T> extends BasicProcessor<C> {

    /**
     * .
     *
     * @param list 来自{@link Context#getItemList()}
     */
    protected abstract Flux<T> reactiveRule(C context, Flux<T> list);

    @Override
    protected Flux<Object> process(C context, List<Flux<Object>> predecessors,
        ProcessorConfig processorConfig) {
        Flux<T> flux = reactiveRule(context, (Flux<T>) predecessors.get(0));
        return flux.map(Function.identity());
    }
}
