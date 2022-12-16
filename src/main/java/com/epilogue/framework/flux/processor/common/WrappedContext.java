package com.epilogue.framework.flux.processor.common;

import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import com.epilogue.framework.flux.processor.core.ProcessorState;
import com.google.common.collect.Maps;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 封装框架的入参Context
 */
@Getter
@Builder
@AllArgsConstructor
public class WrappedContext implements Context {

    private Context realContext;
    private FrameworkConfig frameworkConfig;
    private Map<ProcessorConfig, ProcessorState> stateMap;
    @Setter
    private Throwable throwable;

    /**
     * .
     */
    public WrappedContext(Context realContext, FrameworkConfig frameworkConfig,
        Map<ProcessorConfig, ProcessorState> stateMap) {
        this.realContext = realContext;
        this.frameworkConfig = frameworkConfig;
        this.stateMap = stateMap;
    }

    @Override
    public Map<String, String> requestParams() {
        return realContext.requestParams();
    }

    @Override
    public Map<String, Integer> subChainCount() {
        return realContext.subChainCount();
    }

    public static WrappedContext newContext(Context context, FrameworkConfig frameworkConfig) {
        return WrappedContext.builder().realContext(context).frameworkConfig(frameworkConfig)
            .stateMap(Maps.newConcurrentMap()).build();
    }

    public static WrappedContext newContext(Context context, WrappedContext wrappedContext) {
        return new WrappedContext(context, wrappedContext.getFrameworkConfig(), wrappedContext.getStateMap());
    }
}
