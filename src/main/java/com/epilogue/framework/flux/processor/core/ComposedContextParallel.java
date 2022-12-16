package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * ComposedContextParallel Processor. run chain config in parallel mode.
 */
final class ComposedContextParallel extends BasicProcessor<Context> {

    @Override
    protected Flux<Object> process(Context context, List<Flux<Object>> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("context parallel processor is only for job dispatch");
    }
}
