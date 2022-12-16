package com.epilogue.framework.flux.processor.basic;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import java.util.Objects;
import reactor.core.publisher.Mono;

public abstract class BooleanConditionProcessor<C extends Context> extends ConditionProcessor<C> {

    protected Mono<String> getCondition(C context, Object predecessor,
        ProcessorConfig processorConfig) {
        return getBooleanCondition(context, predecessor, processorConfig)
            .map(Objects::toString).map(String::toLowerCase);
    }

    protected abstract Mono<Boolean> getBooleanCondition(C context, Object predecessor,
        ProcessorConfig processorConfig);
}
