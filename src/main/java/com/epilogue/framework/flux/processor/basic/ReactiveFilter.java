package com.epilogue.framework.flux.processor.basic;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.BasicProcessor;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class ReactiveFilter<C extends Context<T>, T> extends BasicProcessor<C> {

    @Parameter(nullable = true)
    private boolean printSize;

    protected abstract Mono<T> reactiveFilter(C context, T item);

    @Override
    protected Flux<Object> process(C context, List<Flux<Object>> predecessors,
        ProcessorConfig processorConfig) {
        AtomicInteger count = new AtomicInteger();
        return predecessors.get(0)
            .flatMapSequential(item -> reactiveFilter(context, (T) item))
            .doOnNext(x -> {
                if (printSize && log.isInfoEnabled()) {
                    count.getAndIncrement();
                }
            }).doOnComplete(() -> {
                if (printSize && log.isInfoEnabled()) {
                    log.info(String.format("after %s, list size is %d",
                        this.getClass().getSimpleName(), count.get()));
                }
            }).map(Function.identity());
    }
}
