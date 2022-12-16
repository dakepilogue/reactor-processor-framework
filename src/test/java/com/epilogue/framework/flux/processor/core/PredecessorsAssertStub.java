package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import reactor.core.publisher.Flux;

@Slf4j
public class PredecessorsAssertStub extends SingleInputProcessor<Context> {

    @Parameter
    private Object content;

    @Override
    protected Flux<Object> process(Context context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return predecessor.doOnNext(item -> Assert.assertEquals("predecessors result didn't match", content, item));
    }
}
