package com.epilogue.framework.demo.core;

import com.epilogue.framework.processor.annotation.Parameter;
import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.BasicProcessor;
import com.epilogue.framework.processor.core.ProcessorConfig;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class DebugStub extends BasicProcessor<Context> {

    @Parameter
    private String msg;

    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return Mono.fromRunnable(() -> {
            log.info(String.format("DebugStub(%s): %s", Math.random(), msg));
        });
    }
}
