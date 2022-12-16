package com.epilogue.framework.demo.render;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import com.epilogue.framework.flux.processor.core.Render;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

/**
 * .
 */
@Slf4j
public class DummyRender extends Render<Context, Object> {

    @Override
    protected Object render(Context context, List<Object> predecessors,
        ProcessorConfig processorConfig) {

        if (CollectionUtils.isNotEmpty(predecessors)) {
            predecessors.forEach(t -> {
                log.info("predecessor: " + t);
            });
        }
        return dummyResult(context, predecessors);
    }

    private Object dummyResult(Context context, List<Object> predecessors) {
        return "dummy render result";
    }
}
