package com.epilogue.framework.flux.processor.basic;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import com.epilogue.framework.flux.processor.core.WindowProcessor;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;

public class WindowMerger<C extends Context> extends WindowProcessor<C> {

    @Override
    protected Flux<Object> window(C context, List<List<Object>> predecessors,
        ProcessorConfig processorConfig) {
        return Flux.just(predecessors.stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }
}
