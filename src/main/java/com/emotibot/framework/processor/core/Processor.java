package com.emotibot.framework.processor.core;

import com.emotibot.framework.processor.common.Context;
import com.emotibot.framework.processor.common.ProcessorConfig;
import java.util.List;
import reactor.core.publisher.Mono;

interface Processor<C extends Context> {

    Mono<Object> processEntrance(
        C context,
        List<Try<Object>> predecessors,
        ProcessorConfig processorConfig);
}
