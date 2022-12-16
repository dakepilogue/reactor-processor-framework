package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * SubChain Processor. run subChainConfigMap processors in selectorConfig returned values with parallel mode.
 */
final class SubChain extends ExceptionHandlerProcessor<Context> {

    @Override
    Mono<Object> process(Context context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("sub chain processor is only for job dispatch");
    }
}
