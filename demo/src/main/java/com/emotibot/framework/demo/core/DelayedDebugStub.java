package com.epilogue.framework.demo.core;

import static com.epilogue.framework.demo.Constants.UUID;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import com.epilogue.framework.flux.processor.core.SingleInputProcessor;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class DelayedDebugStub extends SingleInputProcessor<Context> {

    private static final ExecutorService executor = Executors.newFixedThreadPool(8);

    public static void shutDown() {
        executor.shutdown();
    }

    @Parameter
    private String msg;

    @Parameter
    private Duration sleep;

    @Override
    protected Flux<Object> process(Context context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return predecessor.flatMap(x -> Mono.create(sink -> executor.execute(() -> {
            if (sink.currentContext().hasKey(UUID)) {
                MDC.put(UUID, sink.currentContext().get(UUID));
            }
            try {
                Thread.sleep(sleep.toMillis());
            } catch (InterruptedException e) {
                sink.error(e);
            }
            log.info(String.format("DebugStub: %s", msg));
            sink.success(msg);
        })));
    }
}