package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.annotation.Parameter;
import com.epilogue.framework.processor.common.Context;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class DelayedDebugStub extends BasicProcessor<Context> {

    private static final ExecutorService executor = Executors.newFixedThreadPool(8);

    @Parameter
    private String msg;

    @Parameter
    private Duration sleep;

    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return Mono.create(sink -> executor.execute(() -> {
            try {
                Thread.sleep(sleep.toMillis());
            } catch (InterruptedException e) {
                sink.error(e);
            }
            if (msg.equals("empty")) {
                sink.success();
                return;
            }
            log.info(String.format("DelayedDebugStub: sleep %sms", sleep.toMillis()));
            sink.success(msg);
        }));
    }
}
