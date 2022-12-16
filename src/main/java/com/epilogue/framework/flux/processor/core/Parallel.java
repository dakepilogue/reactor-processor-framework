package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * Parallel Processor. run childrenConfigs processors in parallel mode.
 */
final class Parallel extends BasicProcessor<Context> {

    @Override
    protected Flux<Object> process(Context context, List<Flux<Object>> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("parallel processor is only for job dispatch");
    }
}
