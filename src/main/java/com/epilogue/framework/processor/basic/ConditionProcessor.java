package com.epilogue.framework.processor.basic;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.BasicProcessor;
import com.epilogue.framework.processor.core.Chain;
import com.epilogue.framework.processor.core.ProcessorConfig;
import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * 抽象的switch条件判断Processor. {@link Chain#switchDispatch}
 */
public abstract class ConditionProcessor<C extends Context> extends BasicProcessor<C> {

    /**
     * 返回执行的switch case name
     */
    protected abstract Mono<String> getCondition(C context, List<Object> predecessors,
        ProcessorConfig processorConfig);

    @Override
    protected Mono<Object> process(C context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return getCondition(context, predecessors, processorConfig).map(Function.identity());
    }
}
