package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Serial Processor. run childrenConfigs processors serially in order.
 */
final class Serial extends BasicProcessor<Context> {

    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("serial processor is only for job dispatch");
    }
}
