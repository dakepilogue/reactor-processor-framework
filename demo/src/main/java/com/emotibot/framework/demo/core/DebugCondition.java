package com.epilogue.framework.demo.core;

import com.epilogue.framework.flux.processor.basic.ConditionProcessor;
import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class DebugCondition extends ConditionProcessor<Context> {

    private static Random random = new Random(System.currentTimeMillis());

    @Override
    protected Mono<String> getCondition(Context context, Object predecessor, ProcessorConfig processorConfig) {
        int n = random.nextInt(2);
        Mono<String> result = Mono.just("value" + (char) ('A' + n));
        return result.doOnNext(log::info);
    }
}
