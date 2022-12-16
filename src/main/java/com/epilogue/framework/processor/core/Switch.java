package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Mono;

final class Switch<C extends Context> extends BasicProcessor<C> {

    @Override
    protected Mono<Object> process(C context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("switch processor is only for job dispatch");
    }
}
