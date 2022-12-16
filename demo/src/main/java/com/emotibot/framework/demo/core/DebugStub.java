package com.epilogue.framework.demo.core;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import com.epilogue.framework.flux.processor.core.SingleInputProcessor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class DebugStub extends SingleInputProcessor<Context> {

    @Parameter
    private String msg;

    @Override
    protected Flux<Object> process(Context context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return predecessor.flatMap(x -> Mono.fromCallable(() -> {
            log.info(String.format("DebugStub(%s): %s", Math.random(), msg));
            return msg;
        }));
    }
}
