package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.basic.BasicMerger;
import com.epilogue.framework.processor.common.Context;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class DebugMerger extends BasicMerger<Context> {

    @Override
    public Mono<Object> merge(Context context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        return super.merge(context, predecessors, processorConfig).doOnNext(res -> {
            if (res instanceof List) {
                Collections.sort((List) res);
            }
        });
    }
}
