package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.basic.ConditionProcessor;
import com.epilogue.framework.flux.processor.common.Context;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class DebugCondition extends ConditionProcessor<Context> {

    @Parameter
    private String value;

    @Override
    protected Mono<String> getCondition(Context context, Object predecessorItem, ProcessorConfig processorConfig) {
        Mono<String> result = Mono.just(value);
        return result.doOnNext(log::info);
    }
}
