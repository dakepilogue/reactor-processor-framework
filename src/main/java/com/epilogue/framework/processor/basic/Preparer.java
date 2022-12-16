package com.epilogue.framework.processor.basic;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.BasicProcessor;
import com.epilogue.framework.processor.core.ProcessorConfig;
import java.util.List;
import reactor.core.publisher.Mono;

public abstract class Preparer<C extends Context> extends BasicProcessor<C> {

    @Override
    protected final Mono<Object> process(C context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return prepare(context, processorConfig);
    }

    protected abstract Mono<Object> prepare(C context, ProcessorConfig processorConfig);
}
