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
public class ParameterDebugStub extends BasicProcessor<Context> {

    @Parameter(simpleClassNamePrefix = false)
    private String msg;

    @Parameter(isClassName = true)
    private MockItem newMockItem;

    @Parameter(isClassName = true, beanName = "mockItem", initializeFromContainer = true)
    private MockItem mockItem;

    @Parameter(isClassName = true, beanName = "mockItem", initializeFromContainer = true)
    private List<MockItem> mockItems;

    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        mockItem.invoke();
        newMockItem.invoke();
        mockItems.forEach(item -> item.invoke());
        return Mono.fromCallable(() -> {
            log.info(String.format("ParameterDebugStub(%s): %s", Math.random(), msg));
            return msg;
        });
    }
}
