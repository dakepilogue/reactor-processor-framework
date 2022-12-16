package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

final class Switch<C extends Context> extends BasicProcessor<C> {

    @Override
    protected Flux<Object> process(C context, List<Flux<Object>> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("switch processor is only for job dispatch");
    }
}
