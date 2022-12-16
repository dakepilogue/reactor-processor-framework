package com.epilogue.framework.demo;

import com.epilogue.framework.processor.basic.BasicMerger;
import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.ProcessorConfig;
import com.epilogue.framework.processor.core.Try;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class DummyMerger extends BasicMerger<Context> {

    @Override
    public Mono<Object> merge(Context context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {
        return super.merge(context, predecessors, processorConfig).doOnNext(res -> log.info(res.toString()));
    }
}
