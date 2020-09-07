package com.emotibot.framework.processor.merger;

import com.emotibot.framework.processor.common.Context;
import com.emotibot.framework.processor.common.ProcessorConfig;
import com.emotibot.framework.processor.core.Merger;
import com.emotibot.framework.processor.core.PredecessorException;
import com.emotibot.framework.processor.core.Try;
import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * SimpleDeliver processor. return predecessors.
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
