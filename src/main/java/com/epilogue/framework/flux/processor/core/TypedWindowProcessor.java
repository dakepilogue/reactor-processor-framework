package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Flux;

public abstract class TypedWindowProcessor<C extends Context, T> extends TypedBasicProcessor<C, T> {

    @Parameter
    private List<Integer> windows;

    @Override
    protected Flux<Object> process(C context, List<Flux<T>> predecessors, ProcessorConfig processorConfig) {
        return mergePredecessors(predecessors).flatMapSequential(pres -> window(context, pres, processorConfig));
    }

    protected abstract Flux<Object> window(C context, List<List<T>> predecessors,
        ProcessorConfig processorConfig);

    /**
     * 合并上一步的processor，在它们都完成之后在执行后续逻辑
     */
    private Flux<List<List<T>>> mergePredecessors(List<Flux<T>> predecessors) {
        List<Flux<List<T>>> pres = Lists.newArrayListWithExpectedSize(predecessors.size());
        for (int i = 0; i < predecessors.size(); i++) {
            Integer window = windows.get(i);
            if (window > 0) {
                pres.add(predecessors.get(i).buffer(window).map(Function.identity()));
            } else {
                pres.add(predecessors.get(i).buffer().map(Function.identity()));
            }
        }
        return Flux.zip(pres, objects -> {
            List<List<T>> res = Lists.newArrayListWithCapacity(objects.length);
            for (Object object : objects) {
                res.add((List<T>) object);
            }
            return res;
        });
    }
}
