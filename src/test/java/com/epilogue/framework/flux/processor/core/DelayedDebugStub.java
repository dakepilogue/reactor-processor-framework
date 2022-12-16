package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class DelayedDebugStub extends SingleInputProcessor<Context> {

    private static final ExecutorService executor = Executors.newFixedThreadPool(8);

    @Parameter
    private String msg;

    @Parameter
    private Duration sleep;

    @Override
    protected Flux<Object> process(Context context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return predecessor.flatMap(x -> Mono.create(sink -> executor.execute(() -> {
            try {
                Thread.sleep(sleep.toMillis());
            } catch (InterruptedException e) {
                sink.error(e);
            }
            if (msg.equals("sleep 100ms")) {
                sink.success();
                return;
            }
            log.info(String.format("DelayedDebugStub: %s", msg));
            sink.success(msg);
        })));
    }
}
