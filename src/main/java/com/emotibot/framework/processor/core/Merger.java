package com.emotibot.framework.processor.core;

import com.emotibot.framework.processor.common.Context;
import com.emotibot.framework.processor.common.ProcessorConfig;
import java.util.List;
import reactor.core.publisher.Mono;

public abstract class Merger<C extends Context> extends ExceptionHandlerProcessor<C> {


    protected abstract Mono<Object> merge(C context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig);

    @Override
    Mono<Object> process(C context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        return merge(context, predecessors, processorConfig);
    }
}
