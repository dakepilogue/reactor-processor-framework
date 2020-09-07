package com.emotibot.framework.processor.core;

import com.emotibot.framework.processor.common.Context;
import com.emotibot.framework.processor.common.ProcessorConfig;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Parallel Processor. run childrenConfigs processors in parallel mode.
 */
public final class Serial extends BasicProcessor<Context> {

    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("serial processor is only for job dispatch");
    }
}
