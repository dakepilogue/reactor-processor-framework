package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.common.Context;
import com.google.common.base.Throwables;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class Render<C extends Context, R> extends ExceptionHandlerProcessor<C> {

    protected abstract R render(C context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig);

    @Override
    final Mono<Object> process(C context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        return Mono.just(render(context, predecessors, processorConfig));
    }

    /**
     * 前置Processor的执行过程中是否包含Exception.
     */
    protected boolean hasError(List<Try<Object>> predecessors) {
        return predecessors.stream().anyMatch(Try::isThrow);
    }


    /**
     * .
     */
    protected String buildError(C context, List<Try<Object>> predecessors) {
        if (hasError(predecessors)) {
            StringBuilder builder = new StringBuilder("\n");
            List<Throwable> errors = predecessors.stream()
                .filter(Try::isThrow)
                .map(Try::getThrowable)
                .collect(Collectors.toList());
            builder.append(String.format(
                "Exception[%d] catch in \"%s\":",
                errors.size(), this.getClass().getName()));
            for (Throwable error : errors) {
                if (PredecessorException.class.isInstance(error)) {
                    PredecessorException predecessorException = (PredecessorException) error;
                    builder.append(predecessorException.logErrorStackToString());
                } else {
                    builder.append(Throwables.getStackTraceAsString(error));
                }
            }
            return builder.toString();
        }
        return null;
    }
}
