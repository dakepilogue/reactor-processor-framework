package com.emotibot.framework.processor.core;

import com.emotibot.framework.processor.common.Context;
import com.emotibot.framework.processor.common.ProcessorConfig;
import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

public abstract class BasicProcessor<C extends Context> implements Processor<C> {

    /**
     * 默认前一级的结果全部成功.
     */
    public final Mono<Object> processEntrance(C context,
        List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        Throwable[] errors = predecessors
            .stream()
            .filter(p -> !p.isReturn())
            .map(Try::getThrowable)
            .toArray(Throwable[]::new);
        if (errors.length > 0) {
            return Mono.error(new PredecessorException(errors, this.getClass()));
        } else {
            return process(
                context,
                predecessors
                    .stream()
                    .map(Try::getReturned)
                    .collect(Collectors.toList()),
                processorConfig);
        }

    }

    protected abstract Mono<Object> process(C context, List<Object> predecessors,
        ProcessorConfig processorConfig);
}
