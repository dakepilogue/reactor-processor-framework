package com.epilogue.framework.flux.processor.basic;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.Chain;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import com.epilogue.framework.flux.processor.core.SingleInputProcessor;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 抽象的subChain selector. {@link Chain#subChainDispatch}
 */
public abstract class SelectorProcessor<C extends Context> extends SingleInputProcessor<C> {

    protected final <T extends Context> Flux<Map<String, T>> getSelectorMap(C context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return predecessor.flatMapSequential(item -> this.getSelectorValues(context, item, processorConfig))
            .map(values -> values.stream().collect(Collectors.toMap(Function.identity(), v -> (T) context)));
    }

    /**
     * 返回执行的subChain name
     */
    protected abstract Mono<Set<String>> getSelectorValues(C context, Object predecessorItem,
        ProcessorConfig processorConfig);

    @Override
    protected Flux<Object> process(C context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return Flux.from(getSelectorMap(context, predecessor, processorConfig));
    }
}
