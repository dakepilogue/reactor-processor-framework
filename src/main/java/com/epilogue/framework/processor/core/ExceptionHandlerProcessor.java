package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.util.LogUtil;
import java.util.List;
import reactor.core.publisher.Mono;

abstract class ExceptionHandlerProcessor<C extends Context> implements Processor<C> {

    /**
     * 将 Try 对象直接传递给 process，使得 process 有能力处理错误.
     */
    public final Mono<Object> processEntrance(C context,
        List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        String uuidKey = processorConfig.getUuidKey();
        return LogUtil.logUuid(uuidKey, () -> process(context, predecessors, processorConfig));
    }

    abstract Mono<Object> process(C context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig);
}