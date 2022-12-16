package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public abstract class Render<C extends Context, R> extends SingleInputProcessor<C> {

    protected abstract R render(C context, List<Object> predecessorItemList,
        ProcessorConfig processorConfig);

    @Override
    protected final Flux<Object> process(C context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return predecessor.buffer().map(list -> render(context, list, processorConfig));
    }
}
