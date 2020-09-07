package com.emotibot.framework.processor;

import com.emotibot.framework.processor.common.Context;
import com.emotibot.framework.processor.common.ProcessorConfig;
import com.emotibot.framework.processor.common.ThreadPool;
import com.emotibot.framework.processor.core.Chain;
import com.emotibot.framework.processor.interfaces.BeanContainer;
import com.emotibot.framework.processor.interfaces.BeanContainer.DefaultBeanContainer;
import com.emotibot.framework.processor.interfaces.ContextBuilder;
import com.emotibot.framework.processor.interfaces.Request;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Slf4j
public class ProcessorFramework {

    private static final int DEFAULT_THREAD_NUMBER = 16;
    private static final String DEFAULT_THREAD_NAME = "logic";
    private final ThreadPool threadPool;
    private final ContextBuilder contextBuilder;
    private final BeanContainer beanContainer;

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
     * 支持并行串行处理逻辑
     *
     * @param contextBuilder 上下文构造器
     * @param beanContainer  bean容器
     * @param threadPool     线程池
     */
    public ProcessorFramework(ContextBuilder contextBuilder, BeanContainer beanContainer, ThreadPool threadPool) {
        this.contextBuilder = contextBuilder;
        this.beanContainer = beanContainer;
        this.threadPool = threadPool;
        beanContainer.registerBean("logic", Scheduler.class, threadPool.getScheduler());
        beanContainer.registerBean("chain", Scheduler.class, threadPool.getTimer());
    }

    /**
     * 运行处理逻辑
     *
     * @param request       请求
     * @param handlerConfig processor配置
     */
    public Mono<Object> process(Request request, Config handlerConfig) {
        Context context = contextBuilder.build(request);
        ProcessorConfig processorConfig = new ProcessorConfig(handlerConfig);
        Chain chain = new Chain(processorConfig, context, beanContainer);
        Mono<Object> result = chain.process();
        return result.map(obj -> {
            if (obj == null) {
                throw new RuntimeException("null returned from Chain");
            }
            return obj;
        }).doOnError(throwable -> {
            log.error(throwable.getMessage(), throwable);
        });
    }

    public void shutDown() {
        this.threadPool.getScheduler().dispose();
        this.threadPool.getTimer().dispose();
    }
}
