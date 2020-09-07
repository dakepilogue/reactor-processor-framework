package com.emotibot.frameword.processor.core;

import com.emotibot.framework.processor.annotation.Parameter;
import com.emotibot.framework.processor.common.Context;
import com.emotibot.framework.processor.common.ProcessorConfig;
import com.emotibot.framework.processor.core.BasicProcessor;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class DelayedDebugStub extends BasicProcessor<Context> {

    @Parameter
    private String msg;

    @Parameter
    private Duration sleep;

    @Override
    protected Mono<Object> process(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {
        return Mono.delay(sleep).map(x -> {
            if (sleep.toMillis() == 200) {
                throw new RuntimeException("error");
            }
            log.info(String.format("DebugStub: %s", msg));
            return predecessors.get(0);
        });
    }
}
