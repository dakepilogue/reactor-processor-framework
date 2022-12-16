package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Flux;

public class ItemProvider extends SingleInputProcessor<Context> {

    @Parameter
    private List<String> items;

    @Override
    protected Flux<Object> process(Context context, Flux<Object> predecessor, ProcessorConfig processorConfig) {
        return predecessor.flatMapIterable(x -> items);
    }
}
