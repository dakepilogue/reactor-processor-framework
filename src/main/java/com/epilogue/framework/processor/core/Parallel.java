package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Parallel Processor. run childrenConfigs processors in parallel mode.
 */
final class Parallel extends ExceptionHandlerProcessor<Context> {

    @Override
    Mono<Object> process(Context context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("parallel processor is only for job dispatch");
    }
}
