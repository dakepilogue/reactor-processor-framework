package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.annotation.Parameter;
import com.epilogue.framework.processor.common.Context;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class ExceptionDebugStub extends BasicProcessor<Context> {

    @Parameter
    private String msg;

    @Parameter
    private String exception;

    @SneakyThrows
    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        if (StringUtils.isNotEmpty(msg)) {
            Throwable ex = (Throwable) Class.forName(exception).getConstructor(String.class).newInstance(msg);
            throw ex;
        }
        return Mono.empty();
    }
}
