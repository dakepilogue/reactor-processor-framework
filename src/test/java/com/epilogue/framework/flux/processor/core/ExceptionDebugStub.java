package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.Context;
import java.lang.reflect.InvocationTargetException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

/**
 * Processor for debug. print debug info to logger.
 */
@Slf4j
public class ExceptionDebugStub extends SingleInputProcessor<Context> {

    @Parameter
    private String msg;

    @Parameter
    private String exception;

    @SneakyThrows
    @Override
    protected Flux<Object> process(Context context, Flux<Object> predecessor,
        ProcessorConfig processorConfig) {
        return predecessor.doOnNext(x -> {
            if (StringUtils.isNotEmpty(msg)) {
                try {
                    RuntimeException ex = (RuntimeException) Class.forName(exception).getConstructor(String.class).newInstance(msg);
                    throw ex;
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
