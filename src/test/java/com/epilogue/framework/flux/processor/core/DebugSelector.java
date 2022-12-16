package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.basic.SelectorProcessor;
import com.epilogue.framework.flux.processor.common.Context;
import com.google.common.collect.Sets;
import java.util.Random;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class DebugSelector extends SelectorProcessor<Context> {

    private static Random random = new Random(System.currentTimeMillis());

    @Override
    protected Mono<Set<String>> getSelectorValues(Context context, Object predecessorItem,
        ProcessorConfig processorConfig) {
        Set<String> res = Sets.newHashSet();
        int n = random.nextInt(3);
        res.add("value" + (char) ('A' + n));
        log.info(res.toString());
        return Mono.just(res);
    }
}
