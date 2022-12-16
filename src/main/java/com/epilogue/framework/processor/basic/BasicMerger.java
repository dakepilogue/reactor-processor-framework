package com.epilogue.framework.processor.basic;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.Merger;
import com.epilogue.framework.processor.core.PredecessorException;
import com.epilogue.framework.processor.core.ProcessorConfig;
import com.epilogue.framework.processor.core.Try;
import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * 并行流的默认merger
 */
public class BasicMerger<C extends Context> extends Merger<C> {

    @Override
    public Mono<Object> merge(C context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        Throwable[] errors = predecessors
            .stream()
            .filter(p -> !p.isReturn())
            .map(Try::getThrowable)
            .toArray(Throwable[]::new);
        if (errors.length > 0) {
            return Mono.error(new PredecessorException(errors, this.getClass()));
        }
        return Mono.just(
            predecessors.stream()
                .map(Try::getReturned)
                .collect(Collectors.toList())
        );
    }
}
