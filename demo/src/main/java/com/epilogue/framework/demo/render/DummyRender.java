package com.epilogue.framework.demo.render;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.core.ProcessorConfig;
import com.epilogue.framework.processor.core.Render;
import com.epilogue.framework.processor.core.Try;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

/**
 * .
 */
@Slf4j
public class DummyRender extends Render<Context, Object> {

    @Override
    protected Object render(Context context, List<Try<Object>> predecessors,
        ProcessorConfig processorConfig) {

        if (CollectionUtils.isNotEmpty(predecessors)) {
            predecessors.forEach(t -> {
                if (t.isThrow()) {
                    log.error(t.getThrowable().getMessage(), t.getThrowable());
                } else {
                    log.info("predecessor: " + t.getReturned());
                }
            });
        }
        return dummyResult(context, predecessors);
    }

    private Object dummyResult(Context context, List<Try<Object>> predecessors) {
        return "dummy render result";
    }
}
