package com.epilogue.framework.processor.basic;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.ProcessorConfig;
import java.util.List;
import java.util.Objects;
import reactor.core.publisher.Mono;

public abstract class BooleanConditionProcessor<C extends Context> extends ConditionProcessor<C> {

    protected Mono<String> getCondition(C context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return getBooleanCondition(context, predecessors, processorConfig)
            .map(Objects::toString).map(String::toLowerCase);
    }

    protected abstract Mono<Boolean> getBooleanCondition(C context, List<Object> predecessors,
        ProcessorConfig processorConfig);
}
