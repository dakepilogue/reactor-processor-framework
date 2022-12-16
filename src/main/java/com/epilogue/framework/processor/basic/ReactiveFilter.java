package com.epilogue.framework.processor.basic;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.BasicProcessor;
import com.epilogue.framework.processor.core.ProcessorConfig;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class ReactiveFilter<C extends Context<T>, T> extends BasicProcessor<C> {

    protected abstract Mono<Collection<T>> reactiveFilter(C context, Collection<T> list);

    @Override
    protected Mono<Object> process(C context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return reactiveFilter(context, context.getItemList())
            .doOnSuccess(list -> {
                context.setItemList(list);
                // 记录过滤后数量
                int afterCnt = CollectionUtils.size(list);
                if (log.isInfoEnabled()) {
                    log.info(String.format("after %s, list size is %d",
                        this.getClass().getSimpleName(), afterCnt));
                }
            }).map(Function.identity());
    }
}
