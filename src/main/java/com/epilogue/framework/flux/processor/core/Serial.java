package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * Serial Processor. run childrenConfigs processors serially in order.
 */
final class Serial extends BasicProcessor<Context> {

    @Override
    protected Flux<Object> process(Context context, List<Flux<Object>> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("serial processor is only for job dispatch");
    }
}
