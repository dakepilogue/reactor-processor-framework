package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.annotation.Parameter;
import com.epilogue.framework.processor.common.Context;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import reactor.core.publisher.Mono;

@Slf4j
public class PredecessorsAssertStub extends BasicProcessor<Context> {

    @Parameter
    private Object content;

    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        Assert.assertEquals("predecessors result didn't match", content, predecessors.get(0));
        return Mono.empty();
    }
}
