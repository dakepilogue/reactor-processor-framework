package com.epilogue.framework.flux.processor;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.common.FrameworkConfig;
import com.epilogue.framework.flux.processor.common.ProcessInfo;
import com.epilogue.framework.flux.processor.common.ThreadPool;
import com.epilogue.framework.flux.processor.core.Chain;
import com.epilogue.framework.flux.processor.core.ProcessorConfig;
import com.epilogue.framework.flux.processor.interfaces.BeanContainer;
import com.epilogue.framework.flux.processor.interfaces.BeanContainer.DefaultBeanContainer;
import com.epilogue.framework.flux.processor.interfaces.ContextBuilder;
import com.epilogue.framework.flux.processor.interfaces.Request;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
public class ProcessorFramework {

    private static final int DEFAULT_THREAD_NUMBER = 16;
    private static final String DEFAULT_THREAD_NAME = "logic";
    private final ThreadPool threadPool;
    private final ContextBuilder contextBuilder;
    @Getter
    private final BeanContainer beanContainer;
    private Map<Integer, ProcessorConfig> configCache = Maps.newConcurrentMap();
    @Setter
    private FrameworkConfig frameworkConfig = FrameworkConfig.DEFAULT;

    public ProcessorFramework(ContextBuilder contextBuilder) {
        this(contextBuilder, new DefaultBeanContainer());
    }

    public ProcessorFramework(ContextBuilder contextBuilder, Scheduler scheduler) {
        this(contextBuilder, new DefaultBeanContainer(), scheduler);
    }

    public ProcessorFramework(ContextBuilder contextBuilder, BeanContainer beanContainer) {
        this(contextBuilder, beanContainer, new ThreadPool(DEFAULT_THREAD_NUMBER, DEFAULT_THREAD_NAME));
    }

    public ProcessorFramework(ContextBuilder contextBuilder, BeanContainer beanContainer, Scheduler scheduler) {
        this(contextBuilder, beanContainer, new ThreadPool(scheduler));
    }

    public ProcessorFramework(ContextBuilder contextBuilder, ThreadPool threadPool) {
        this(contextBuilder, new DefaultBeanContainer(), threadPool);
    }

    /**
     * 支持并行串行及嵌套的处理逻辑
     *
     * @param contextBuilder 上下文构造器
     * @param beanContainer  bean容器
     * @param threadPool     线程池
     */
    public ProcessorFramework(ContextBuilder contextBuilder, BeanContainer beanContainer, ThreadPool threadPool) {
        this.contextBuilder = contextBuilder;
        this.beanContainer = beanContainer;
        this.threadPool = threadPool;
        beanContainer.registerBean("rpf-logic", Scheduler.class, threadPool.getScheduler());
        beanContainer.registerBean("rpf-timer", Scheduler.class, threadPool.getTimer());
    }

    /**
     * 运行处理逻辑
     *
     * @param context       上下文对象
     * @param handlerConfig processor配置，若要提前resolve，务必调用{@link ProcessorFramework#resolveConfig}接口
     */
    public Flux<Object> process(Context context, Config handlerConfig) {
        ProcessorConfig processorConfig = parseConfig(handlerConfig);
        return innerProcess(context, processorConfig).map(Tuple2::getT1);
    }

    /**
     * 运行处理逻辑
     *
     * @param request       请求
     * @param handlerConfig processor配置，若要提前resolve，务必调用{@link ProcessorFramework#resolveConfig}接口
     */
    public Flux<Object> process(Request request, Config handlerConfig) {
        return process(contextBuilder.build(request), handlerConfig);
    }

    /**
     * 运行处理逻辑并返回运行信息
     *
     * @param context       上下文对象
     * @param handlerConfig processor配置，若要提前resolve，务必调用{@link ProcessorFramework#resolveConfig}接口
     */
    public Flux<Tuple2<Object, ProcessInfo>> processWithInfo(Context context, Config handlerConfig) {
        ProcessorConfig processorConfig = parseConfig(handlerConfig);
        return innerProcess(context, processorConfig)
            // 与ProcessInfo zip后一起返回
            .map(tuple -> Tuples.of(tuple.getT1(), tuple.getT2().getProcessInfo()));
    }

    /**
     * 运行处理逻辑并返回运行信息
     *
     * @param request       请求
     * @param handlerConfig processor配置，若要提前resolve，务必调用{@link ProcessorFramework#resolveConfig}接口
     */
    public Flux<Tuple2<Object, ProcessInfo>> processWithInfo(Request request, Config handlerConfig) {
        return processWithInfo(contextBuilder.build(request), handlerConfig);
    }

    /**
     * 解析config中的basic、serial、parallel、switch等参数
     *
     * @param config 待resolve的config
     */
    public Config resolveConfig(Config config) {
        return config.withFallback(FrameworkConfig.COMMON_CONFIG).resolve();
    }

    private ProcessorConfig parseConfig(Config handlerConfig) {
        if (!handlerConfig.isResolved()) {
            handlerConfig = resolveConfig(handlerConfig);
        }
        int identity = System.identityHashCode(handlerConfig);
        ProcessorConfig processorConfig = configCache.get(identity);
        if (processorConfig != null) {
            return processorConfig;
        }
        processorConfig = ProcessorConfig.create(handlerConfig);
        configCache.put(identity, processorConfig);
        return ProcessorConfig.create(handlerConfig);
    }

    private Flux<Tuple2<Object, Chain>> innerProcess(Context context, ProcessorConfig processorConfig) {
        Chain chain = new Chain(processorConfig, context, beanContainer, frameworkConfig);
        Flux<Object> result = chain.process();
        return result.map(obj -> {
            if (obj == null) {
                throw new RuntimeException("null returned from Chain");
            }
            return Tuples.of(obj, chain);
        });
    }

    public void shutDown() {
        this.threadPool.getScheduler().dispose();
        this.threadPool.getTimer().dispose();
    }
}
