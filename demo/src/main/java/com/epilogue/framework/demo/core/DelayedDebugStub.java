package com.epilogue.framework.demo.core;

import static com.epilogue.framework.demo.Constants.UUID;

import com.epilogue.framework.processor.annotation.Parameter;
import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.BasicProcessor;
import com.epilogue.framework.processor.core.ProcessorConfig;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class DelayedDebugStub extends BasicProcessor<Context> {

    private static ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void shutDown() {
        executor.shutdown();
    }

    @Parameter
    private String msg;

    @Parameter
    private Duration sleep;

    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return Mono.create(sink -> executor.execute(() -> {
            if (sink.currentContext().hasKey(UUID)) {
                MDC.put(UUID, sink.currentContext().get(UUID));
            }
            try {
                Thread.sleep(sleep.toMillis());
            } catch (InterruptedException e) {
                sink.error(e);
            }
            if (msg.equals("sleep 100ms")) {
                sink.success();
                return;
            }
            log.info(String.format("DebugStub: %s", msg));
            sink.success(msg);
        }));
    }
}
