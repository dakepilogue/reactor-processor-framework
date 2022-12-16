package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

interface IProcessor<C extends Context, T> {

    Flux<Object> processEntrance(
        C context,
        List<Flux<T>> predecessors,
        ProcessorConfig processorConfig);
}
