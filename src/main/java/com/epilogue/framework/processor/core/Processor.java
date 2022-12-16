package com.epilogue.framework.processor.core;

import com.epilogue.framework.processor.common.Context;
import java.util.List;
import reactor.core.publisher.Mono;

interface Processor<C extends Context> {

    Mono<Object> processEntrance(
        C context,
        List<Try<Object>> predecessors,
        ProcessorConfig processorConfig);
}
