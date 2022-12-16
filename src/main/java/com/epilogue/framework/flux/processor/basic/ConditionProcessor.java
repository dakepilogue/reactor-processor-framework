package com.epilogue.framework.flux.processor.basic;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.Chain;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import com.epilogue.framework.flux.processor.core.SingleInputProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

/**
 * 抽象的switch条件判断Processor. {@link Chain#switchDispatch}
 */
public abstract class ConditionProcessor<C extends Context> extends SingleInputProcessor<C> {

    /**
     * 返回执行的switch case name
     */
    protected abstract Mono<String> getCondition(C context, Object predecessorItem,
        ProcessorConfig processorConfig);

    @Override
    protected Flux<Object> process(C context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return predecessor.flatMapSequential(item -> getCondition(context, item, processorConfig)
            .map(condition -> Tuples.of(condition, item)));
    }
}
