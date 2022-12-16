package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.MockItem;
import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class DebugWindowStub extends SingleInputWindowProcessor<Context> {

    @Parameter
    private String msg;

    @Parameter(isClassName = true, beanName = "mockItem", initializeFromContainer = true, nullable = true)
    private MockItem mockItem;

    @Override
    protected Flux<Object> window(Context context, List<Object> predecessor,
        ProcessorConfig processorConfig) {
        if (mockItem != null) {
            mockItem.invoke();
        }
        return Flux.from(Mono.fromCallable(() -> {
            log.info(String.format("DebugStub(%s): %s", Math.random(), msg));
            return msg;
        }));
    }
}
