package com.epilogue.framework.flux.processor.basic;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.BasicProcessor;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * 并行流的默认merger
 */
public class BasicMerger<C extends Context> extends BasicProcessor<C> {

    @Override
    protected Flux<Object> process(C context, List<Flux<Object>> predecessors,
        ProcessorConfig processorConfig) {
        return Flux.merge(predecessors);
    }
}
