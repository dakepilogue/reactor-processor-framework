package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.MockItem;
import com.epilogue.framework.processor.annotation.Parameter;
import com.epilogue.framework.processor.common.Context;
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

    @Parameter(isClassName = true, beanName = "mockItem", initializeFromContainer = true, nullable = true)
    private MockItem mockItem;

    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        if (mockItem != null) {
            mockItem.invoke();
        }
        return Mono.fromCallable(() -> {
            log.info(String.format("DebugStub(%s): %s", Math.random(), msg));
            return msg;
        });
    }
}
