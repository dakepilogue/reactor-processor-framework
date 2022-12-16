package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.util.LogUtil;
import java.util.List;
import reactor.core.publisher.Flux;

public abstract class TypedBasicProcessor<C extends Context, T> implements IProcessor<C, T> {

    /**
     * 默认前一级的结果全部成功.
     */
    public final Flux<Object> processEntrance(C context,
        List<Flux<T>> predecessors,
        ProcessorConfig processorConfig) {
        return LogUtil.logUuid(
            processorConfig.getUuidKey(),
            () -> process(context, predecessors, processorConfig)
        );
    }

    protected abstract Flux<Object> process(C context, List<Flux<T>> predecessors,
        ProcessorConfig processorConfig);
}
