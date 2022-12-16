package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * ComposedContextParallel Processor. run chain config in parallel mode.
 */
final class ComposedContextParallel extends ExceptionHandlerProcessor<Context> {

    @Override
    Mono<Object> process(Context context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("context parallel processor is only for job dispatch");
    }
}
