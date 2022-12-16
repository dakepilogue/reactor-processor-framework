package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.annotation.Parameter;
import com.epilogue.framework.processor.basic.ConditionProcessor;
import com.epilogue.framework.processor.common.Context;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class DebugCondition extends ConditionProcessor<Context> {

    @Parameter
    private String value;

    @Override
    protected Mono<String> getCondition(Context context, List<Object> predecessors, ProcessorConfig processorConfig) {
        Mono<String> result = Mono.just(value);
        return result.doOnNext(log::info);
    }
}
