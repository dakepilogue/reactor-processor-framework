package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.MockItem;
import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class ParameterDebugStub extends SingleInputProcessor<Context> {

    @Parameter(simpleClassNamePrefix = false)
    private String msg;

    @Parameter(isClassName = true)
    private MockItem newMockItem;

    @Parameter(isClassName = true, beanName = "mockItem", initializeFromContainer = true)
    private MockItem mockItem;

    @Parameter(isClassName = true, beanName = "mockItem", initializeFromContainer = true)
    private List<MockItem> mockItems;

    @Override
    protected Flux<Object> process(Context context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return predecessor.map(x -> {
            mockItem.invoke();
            newMockItem.invoke();
            mockItems.forEach(item -> item.invoke());
            log.info(String.format("ParameterDebugStub(%s): %s", Math.random(), msg));
            return msg;
        });
    }
}
