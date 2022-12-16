package com.epilogue.framework.processor.basic;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.BasicProcessor;
import com.epilogue.framework.processor.core.ProcessorConfig;
import com.epilogue.framework.processor.core.Chain;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * 抽象的subChain selector. {@link Chain#subChainDispatch}
 */
public abstract class SelectorProcessor<C extends Context> extends BasicProcessor<C> {

    protected final <T extends Context> Mono<Map<String, T>> getSelectorMap(C context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return this.getSelectorValues(context, predecessors, processorConfig).map(values ->
            values.stream().collect(Collectors.toMap(Function.identity(), v -> (T) context)));
    }

    /**
     * 返回执行的subChain name
     */
    protected abstract Mono<Set<String>> getSelectorValues(C context, List<Object> predecessors,
        ProcessorConfig processorConfig);

    @Override
    protected Mono<Object> process(C context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return getSelectorMap(context, predecessors, processorConfig).map(Function.identity());
    }
}
