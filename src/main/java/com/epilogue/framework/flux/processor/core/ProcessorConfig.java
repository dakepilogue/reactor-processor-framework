package com.epilogue.framework.flux.processor.core;

import static com.epilogue.framework.flux.processor.util.Keys.BEAN;
import static com.epilogue.framework.flux.processor.util.Keys.CLASS;
import static com.epilogue.framework.flux.processor.util.Keys.LATENCY;
import static com.epilogue.framework.flux.processor.util.Keys.TIMEOUT;
import static com.epilogue.framework.flux.processor.util.Keys.WINDOWS;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.FrameworkConfig;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.typesafe.config.Config;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * stateless config (will be cached) all inherited class should be stateless. all running state goes to {@link
 * ProcessorState}
 */
@Slf4j
public class ProcessorConfig {

    @Expose(serialize = false)
    public final String beanName;
    public final String className;
    public final String simplifiedClassName;
    public final Duration timeout;
    public final List<Integer> windows;
    public final Class<? extends IProcessor> type;
    public final List<Tuple2<Field, Parameter>> paramFields;
    @Expose(serialize = false)
    private final Config config;

    FrameworkConfig frameworkConfig;

    /**
     * parse {@link ProcessorConfig}.
     *
     * @param config config
     */
    public ProcessorConfig(Config config) {
        this.config = config;
        this.className = config.getString(CLASS);
        this.type = getProcessorType(this.className);
        this.simplifiedClassName = this.type.getSimpleName();
        this.paramFields = FieldUtils.getAllFieldsList(this.type).stream()
            .filter(field -> field.getAnnotation(Parameter.class) != null)
            .peek(field -> field.getGenericType())  // 避免GenericType延迟初始化导致的并发问题
            .peek(field -> field.setAccessible(true))
            .map(field -> Tuples.of(field, field.getAnnotation(Parameter.class)))
            .collect(Collectors.toList());
        if (config.hasPath(BEAN)) {
            this.beanName = config.getString(BEAN);
        } else {
            this.beanName = null;
        }
        if (config.hasPath(WINDOWS)) {
            this.windows = config.getIntList(WINDOWS);
        } else {
            this.windows = ImmutableList.of();
        }
        this.timeout = config.getDuration(TIMEOUT);
    }

    /**
     * parse {@link ProcessorConfig}.
     *
     * @param config config
     */
    public static ProcessorConfig create(Config config) {
        final Class<? extends IProcessor> type = getProcessorType(config.getString(CLASS));
        final ProcessorConfig processorConfig;
        if (Serial.class.isAssignableFrom(type)) {
            processorConfig = new SerialProcessorConfig(config);
        } else if (Parallel.class.isAssignableFrom(type)) {
            processorConfig = new ParallelProcessorConfig(config);
        } else if (Switch.class.isAssignableFrom(type)) {
            processorConfig = new SwitchProcessorConfig(config);
        } else if (ComposedContextParallel.class.isAssignableFrom(type)) {
            processorConfig = new ContextParallelProcessorConfig(config);
        } else if (SubChain.class.isAssignableFrom(type)) {
            processorConfig = new SubChainProcessorConfig(config);
        } else {
            processorConfig = new ProcessorConfig(config);
        }
        return processorConfig;
    }

    /**
     * 获取processor的类型
     */
    public static Class<? extends IProcessor> getProcessorType(String className) {
        Class<? extends IProcessor> processorType;
        try {
            Class type = Class.forName(className);
            if (!IProcessor.class.isAssignableFrom(type)) {
                throw new RuntimeException(
                    String.format("class \"%s\" is not a IProcessor.", className));
            }
            processorType = (Class<? extends IProcessor>) type;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("class \"%s\" not fond.", className), e);
        }
        return processorType;
    }

    public String getUuidKey() {
        return frameworkConfig.getUuidKey();
    }

    public Config getConfig() {
        return config;
    }

    /**
     * json序列化
     */
    public JsonObject toJsonObject(boolean simplified,
        Map<ProcessorConfig, ProcessorState> stateMap) {
        final JsonObject root = new JsonObject();
        root.addProperty(CLASS, simplified ? simplifiedClassName : className);
        if (!simplified) {
            root.addProperty(TIMEOUT, timeout.toMillis());
        }
        ProcessorState processorState = stateMap.get(this);
        root.addProperty(LATENCY, processorState.getLatency().orElse(-1L));
        return root;
    }
}
