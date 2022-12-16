package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * SubChain Processor. run subChainConfigMap processors in selectorConfig returned values with parallel mode.
 */
final class SubChain extends BasicProcessor<Context> {

    @Override
    protected Flux<Object> process(Context context, List<Flux<Object>> predecessors,
        ProcessorConfig processorConfig) {
        throw new RuntimeException("sub chain processor is only for job dispatch");
    }
}
