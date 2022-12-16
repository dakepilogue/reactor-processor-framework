package com.epilogue.framework.flux.processor.core;

import com.epilogue.framework.flux.processor.annotation.Parameter;
import com.epilogue.framework.flux.processor.common.ComposedContext;
import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.common.FrameworkConfig;
import com.epilogue.framework.flux.processor.common.ProcessInfo;
import com.epilogue.framework.flux.processor.common.WrappedContext;
import com.epilogue.framework.flux.processor.interfaces.BeanContainer;
import com.epilogue.framework.flux.processor.interfaces.Request;
import com.epilogue.framework.flux.processor.util.GsonUtil;
import com.epilogue.framework.flux.processor.util.Keys.Latch;
import com.epilogue.framework.flux.processor.util.LogUtil;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValue;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.function.Tuple2;

@Slf4j
public class Chain {

    private final ProcessorConfig mainProcessorConfig;
    private final FrameworkConfig frameworkConfig;
    private final WrappedContext context;
    private final Scheduler pool;
    private final Scheduler timer;
    private final BeanContainer beanContainer;
    private final Function<Flux<Object>, Flux<Try<Object>>> transformer;

    /**
     * .
     */
    public Chain(ProcessorConfig mainProcessorConfig, Context context, BeanContainer beanContainer,
        FrameworkConfig frameworkConfig) {
        this.context = WrappedContext.newContext(context, frameworkConfig);
        this.mainProcessorConfig = mainProcessorConfig;
        this.frameworkConfig = frameworkConfig;
        this.beanContainer = beanContainer;
        this.pool = beanContainer.getBean("rpf-logic", Scheduler.class);
        this.timer = beanContainer.getBean("rpf-timer", Scheduler.class);
        // 封装返回的对象或异常到Try对象中
        transformer = mono -> mono.onErrorResume(throwable -> Mono.just(new Try<>(throwable)))
            .map(x -> x instanceof Try ? (Try<Object>) x : new Try<>(x));
    }

    /**
     * 执行
     */
    public Flux<Object> process() {
        Flux<Object> empty = frameworkConfig.isPublishOnNewThreadWhenStart()
            ? publishOnPool(Flux.just(StringUtils.EMPTY)) : Flux.just(StringUtils.EMPTY);
        return dispatch(ImmutableList.of(empty), mainProcessorConfig, context)
            .flatMapSequential(LogUtil.logUuid(frameworkConfig.getUuidKey()))
            .switchIfEmpty(Mono.create(sink -> {
                if (context.getThrowable() != null
                    && isInErrors(context.getThrowable(), frameworkConfig.getErrorBlackList())) {
                    sink.error(context.getThrowable());
                } else {
                    sink.success();
                }
            }));
    }

    /**
     * 获取执行信息
     */
    public ProcessInfo getProcessInfo() {
        JsonObject timeInfo = mainProcessorConfig.toJsonObject(frameworkConfig.isSimplifiedTimeInfo(),
            context.getStateMap());
        return new ProcessInfo(timeInfo);
    }

    /**
     * 根据 {@link ProcessorConfig} 调度处理流程
     * <p>
     * 前置为BasicProcessor，则predecessors为前置的BasicProcessor；<br/> 前置为Serial，则predecessors为Serial的最后一个processor；<br/>
     * 前置为Parallel，则predecessors为Parallel的merger processor；<br/>
     * 前置为ComposedContextParallel，则predecessors为ComposedContextParallel执行的所有merger；<br/>
     * 前置为Switch，则predecessors为Switch命中的case；<br/> 如果是Parallel的merger，则predecessors为Parallel的所有的children；<br/>
     * 如果是Switch的case，则predecessors为Switch的condition；
     * </p>
     *
     * @param predecessors 前置调用的processors。
     * @param config       processor调用配置
     * @param context      上下文
     */
    private Flux<Object> dispatch(List<Flux<Object>> predecessors, ProcessorConfig config, WrappedContext context) {
        Class<? extends IProcessor> processorType = config.type;
        Flux<Object> preFuture;
        boolean single = false;
        if (Parallel.class.isAssignableFrom(processorType)) {
            preFuture = parallelDispatch(predecessors, (ParallelProcessorConfig) config, context);
        } else if (Serial.class.isAssignableFrom(processorType)) {
            preFuture = serialDispatch(predecessors, (SerialProcessorConfig) config, context);
        } else if (Switch.class.isAssignableFrom(processorType)) {
            preFuture = switchDispatch(predecessors, (SwitchProcessorConfig) config, context);
        } else if (ComposedContextParallel.class.isAssignableFrom(processorType)) {
            preFuture = composedContextParallelDispatch(predecessors, (ContextParallelProcessorConfig) config, context);
        } else if (SubChain.class.isAssignableFrom(processorType)) {
            preFuture = subChainDispatch(predecessors, (SubChainProcessorConfig) config, context);
        } else {
            single = true;
            preFuture = singleDispatch(predecessors, config, processorType, context);
        }
        preFuture = config.timeout.isNegative() ? preFuture : preFuture.timeout(config.timeout, timer);
        FrameworkConfig frameworkConfig = context.getFrameworkConfig();
        final boolean isSingle = single;
        return preFuture.onErrorResume(
            throwable -> frameworkConfig.isOnErrorContinue()
                && !isInErrors(throwable, frameworkConfig.getErrorBlackList()),
            throwable -> LogUtil.logUuid(frameworkConfig.getUuidKey(),
                () -> Flux.from(Mono.fromRunnable(() -> {
                    handleThrowable(throwable, config, context, isSingle);
                }))
            )
        ).doOnError(throwable -> handleThrowable(throwable, config, context, isSingle));
    }

    private boolean isInErrors(Throwable throwable, Set<Class<? extends Throwable>> errorBlackList) {
        if (CollectionUtils.isEmpty(errorBlackList)) {
            return false;
        }
        while (throwable != null) {
            Class<? extends Throwable> clazz = throwable.getClass();
            if (clazz.equals(PredecessorException.class) && !errorBlackList.contains(clazz)) {
                throwable = throwable.getCause();
                continue;
            }
            if (isInErrors(clazz, errorBlackList)) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    private boolean isInErrors(Class<? extends Throwable> clazz, Set<Class<? extends Throwable>> errorBlackList) {
        while (!Object.class.equals(clazz)) {
            if (errorBlackList.contains(clazz)) {
                return true;
            }
            clazz = (Class<? extends Throwable>) clazz.getSuperclass();
        }
        return false;
    }

    private void handleThrowable(Throwable throwable, ProcessorConfig config,
        WrappedContext context, boolean isSingle) {
        if (context.getThrowable() == throwable) {
            return;
        }
        if (!PredecessorException.class.isInstance(throwable)) {
            String error = String.format(
                "\"%s\" fail, %s",
                config.className,
                config.getConfig().origin().toString()
            );
            if (!isSingle) {
                log.error(error);
                return;
            }
            context.setThrowable(throwable);
            throwable = trimThrowable(throwable, context.getFrameworkConfig().getErrorStackTraceDepth());
            log.error(error, throwable);
        }
        return;
    }

    private Throwable trimThrowable(Throwable throwable, int depth) {
        if (depth > 0) {
            throwable = Throwables.getRootCause(throwable);
            throwable.setStackTrace(ArrayUtils.subarray(throwable.getStackTrace(), 0, depth));
        }
        return throwable;
    }

    /**
     * 单个processor的调用逻辑
     *
     * @param processorType processor类型
     * @param context       上下文信息
     */
    private Flux<Object> singleDispatch(List<Flux<Object>> predecessors,
        ProcessorConfig config, Class<? extends IProcessor> processorType, WrappedContext context) {
        // 将上一步processor的执行情况和返回结果封装在Try里面
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        predecessors = beforeProcess(predecessors, processorState);
        IProcessor processor;
        if (config.beanName != null) {
            processor = beanContainer.getBean(config.beanName, processorType);
        } else {
            processor = beanContainer.getBean(processorType);
        }
        injectParam(processor, config, context);
        config.frameworkConfig = frameworkConfig;
        return processor.processEntrance(context.getRealContext(), predecessors, config)
            .doOnNext(x -> processorState.afterProcess())
            .doOnError(x -> processorState.afterProcess());
    }

    /**
     * 串行processor的调用逻辑
     */
    private Flux<Object> serialDispatch(List<Flux<Object>> predecessors,
        SerialProcessorConfig config, WrappedContext context) {
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        // 将所有children节点链式地串起来
        for (int i = 0; i < config.getChildrenConfigs().size(); i++) {
            ProcessorConfig childConfig = config.getChildrenConfigs().get(i);
            Flux<Object> childFuture;
            if (i == 0) {
                predecessors = beforeProcess(predecessors, processorState);
                childFuture = dispatch(predecessors, childConfig, context);
            } else {
                childFuture = dispatch(predecessors, childConfig, context);
            }
            if (i == config.getChildrenConfigs().size() - 1) {
                // 最后一个child要记录串行的结束时间
                childFuture = childFuture
                    .doOnNext(x -> processorState.afterProcess())
                    .doOnError(x -> processorState.afterProcess());
            }
            // 当前child的future为下一个child的前置
            predecessors = ImmutableList.of(childFuture);
        }
        return predecessors.get(0);
    }

    /**
     * 并行processor的调用逻辑
     */
    private Flux<Object> parallelDispatch(List<Flux<Object>> predecessors,
        ParallelProcessorConfig config, WrappedContext context) {
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        List<ProcessorConfig> childrenConfigs = config.getChildrenConfigs();
        predecessors = beforeAndCacheProcess(predecessors, processorState, childrenConfigs.size());
        List<Flux<Object>> fluxList = Lists.newArrayListWithExpectedSize(childrenConfigs.size());
        // 第一个child的执行沿用上一个processor的线程
        fluxList.add(dispatch(predecessors, childrenConfigs.get(0), context));
        // 其余child在新线程上执行
        for (int i = 1; i < childrenConfigs.size(); i++) {
            fluxList.add(dispatch(publishOnPool(predecessors), childrenConfigs.get(i), context));
        }
        String latch = StringUtils.isEmpty(config.getLatch()) ? Latch.ALL : config.getLatch().toLowerCase();
        // children并行之后执行merger
        return dispatch(fluxList, config.getMergerConfig(), context)
            .doOnNext(x -> processorState.afterProcess())
            .doOnError(x -> processorState.afterProcess());
    }

    /**
     * switch的调用逻辑
     */
    private Flux<Object> switchDispatch(List<Flux<Object>> predecessors,
        SwitchProcessorConfig config, WrappedContext context) {
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        ProcessorConfig conditionProcessorConfig = config.getConditionConfig();
        predecessors = beforeProcess(predecessors, processorState);
        // 先执行condition
        return dispatch(predecessors, conditionProcessorConfig, context)
            .map(ret -> {
                if (ret instanceof Tuple2) {
                    Object conditionValue = ((Tuple2) ret).getT1();
                    if (conditionValue == null) {
                        throw new RuntimeException("found condition as \"null\" not String");
                    } else if (!String.class.isInstance(conditionValue)) {
                        throw new RuntimeException(String.format(
                            "found condition as \"%s\" not String",
                            conditionValue.getClass().getName()));
                    }
                    return (Tuple2<String, Object>) ret;
                } else {
                    throw new RuntimeException("condition processor must return tuple2");
                }
            })
            .groupBy(Tuple2::getT1).flatMapSequential(groupedFlux -> {
                String key = groupedFlux.key();
                processorState.conditionValue = key;
                if (!config.getCasesConfigMap().containsKey(key)) {
                    return Mono.error(new RuntimeException(String.format(
                        "can not found condition \"%s\"",
                        key)));
                }
                Flux<Object> preFlux = groupedFlux.map(Tuple2::getT2);
                // 根据condition执行对应的case
                ProcessorConfig caseProcessorConfig = config.getCasesConfigMap().get(key);
                return dispatch(ImmutableList.of(preFlux), caseProcessorConfig, context);
            })
            .doOnNext(x -> processorState.afterProcess())
            .doOnError(x -> processorState.afterProcess());
    }

    /**
     * 批量请求的调用逻辑
     */
    private Flux<Object> composedContextParallelDispatch(List<Flux<Object>> predecessors,
        ContextParallelProcessorConfig config, WrappedContext context) {
        if (!(context.getRealContext() instanceof ComposedContext)) {
            throw new RuntimeException(
                String.format("context is not a ComposedContext."));
        }
        ComposedContext<? extends Request> composedContext =
            (ComposedContext<? extends Request>) context.getRealContext();
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        if (composedContext.getContexts().size() == 0) {
            return Flux.empty();
        }
        Integer referCount = composedContext.getContexts().size();
        predecessors = beforeAndCacheProcess(predecessors, processorState, referCount);
        List<Flux<Object>> fluxList = Lists.newArrayListWithExpectedSize(referCount);
        // 每个子context执行一次chain config，并且是并行执行
        fluxList.add(dispatch(predecessors, config.getChainConfig(),
            WrappedContext.newContext(composedContext.getContexts().get(0), context)));
        for (int i = 1; i < composedContext.getContexts().size(); i++) {
            fluxList.add(dispatch(publishOnPool(predecessors), config.getChainConfig(),
                WrappedContext.newContext(composedContext.getContexts().get(i), context)));
        }
        return Flux.mergeSequential(fluxList)
            .doOnNext(x -> processorState.afterProcess())
            .doOnError(x -> processorState.afterProcess());
    }

    /**
     * subChain的调用逻辑。
     * <p>
     * 根据selector返回的多个值，然后并行去执行subChain下面对应的多个子chain，最后再merge多个chain返回的结果
     * </p>
     */
    private Flux<Object> subChainDispatch(List<Flux<Object>> predecessors,
        SubChainProcessorConfig config, WrappedContext context) {
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        ProcessorConfig selectorProcessorConfig = config.getSelectorConfig();
        List<Flux<Object>> pres = beforeAndCacheProcess(predecessors, processorState, null);
        // 先执行selector
        Flux<Object> selectorFlux = dispatch(pres, selectorProcessorConfig, context);
        return selectorFlux.flatMapSequential(selectorValue -> {
            if (selectorValue == null) {
                return Mono.error(new RuntimeException("found selectorValue as \"null\" not String"));
            } else if (!Map.class.isInstance(selectorValue)) {
                return Mono.error(new RuntimeException(String.format(
                    "found selectorValue as \"%s\" not Map",
                    selectorValue.getClass().getName())));
            }
            Map<String, Object> values = (Map<String, Object>) selectorValue;
            processorState.selectorValues = ImmutableList.copyOf(values.keySet());
            Map<Integer, Flux<Object>> fluxMap = Maps.newHashMap();
            Map<String, ProcessorConfig> subChainConfigMap = config.getSubChainConfigMap();
            int i = 0;
            for (String key : processorState.selectorValues) {
                if (!subChainConfigMap.containsKey(key)) {
                    return Mono.error(new RuntimeException(String.format(
                        "can not found sub chain for selectorValue \"%s\"",
                        key)));
                }
                ProcessorConfig processorConfig = subChainConfigMap.get(key);
                Object subChainContext = values.get(key);
                if (subChainContext instanceof Context) {
                    subChainDispatch(pres, processorConfig, (Context) subChainContext, context, fluxMap, i);
                    i++;
                } else if (subChainContext instanceof List) {
                    List subChainContexts = (List) subChainContext;
                    for (Object chainContext : subChainContexts) {
                        if (chainContext instanceof Context) {
                            subChainDispatch(pres, processorConfig, (Context) chainContext, context, fluxMap, i);
                            i++;
                        }
                    }
                } else {
                    return Mono.error(new IllegalStateException(String.format(
                        "context returned from selector must be 'Context' or 'List', selectorValue is \"%s\"",
                        key)));
                }
            }
            List<Flux<Object>> fluxList = Lists.newArrayListWithExpectedSize(fluxMap.size());
            for (int j = 0; j <= fluxMap.keySet().stream().max(Integer::compareTo).get(); j++) {
                fluxList.add(fluxMap.getOrDefault(j, Flux.empty()));
            }
            // subChain并行之后执行merger
            return dispatch(fluxList, config.getMergerConfig(), context);
        }).doOnNext(x -> processorState.afterProcess()).doOnError(x -> processorState.afterProcess());
    }

    private void subChainDispatch(List<Flux<Object>> pre, ProcessorConfig config, Context context,
        WrappedContext wrappedContext, Map<Integer, Flux<Object>> fluxMap, int i) {
        WrappedContext subContext = WrappedContext.newContext(context, wrappedContext);
        Flux<Object> flux;
        if (i == 0) {
            // 第一个subChain的执行沿用上一个processor的线程
            flux = dispatch(pre, config, subContext);
        } else {
            // 其余subChain在新线程上执行
            flux = dispatch(publishOnPool(pre), config, subContext);
        }
        // 获取该subChain对应的index，如果有相同index的subChain会merge到一起
        Integer index = context.subChainIndex();
        if (index == null) {
            index = fluxMap.size();
        }
        fluxMap.compute(index, (idx, prev) -> prev == null ? flux : prev.mergeWith(flux));
    }

    private void appendFlux(Map<Integer, Flux<Object>> fluxMap, Integer index, Flux<Object> flux) {
        fluxMap.compute(index, (idx, prev) -> prev == null ? flux : prev.mergeWith(flux));
    }

    private <T> List<Flux<T>> publishOnPool(List<Flux<T>> fluxList) {
        return fluxList.stream().map(this::publishOnPool).collect(Collectors.toList());
    }

    private <T> Flux<T> publishOnPool(Flux<T> flux) {
        return flux.publishOn(pool) // 后续在新线程中执行
            .flatMapSequential(LogUtil.logUuid(frameworkConfig.getUuidKey()));
    }

    /**
     * 合并上一步的processor，在它们都完成之后在执行后续逻辑
     */
    private List<Flux<Object>> beforeProcess(List<Flux<Object>> predecessors, ProcessorState state) {
        return predecessors.stream().map(predecessor -> predecessor.doOnNext(x -> state.beforeProcess()))
            .collect(Collectors.toList());
    }

    /**
     * 合并上一步的processor，在它们都完成之后在执行后续逻辑
     */
    private List<Flux<Object>> beforeAndCacheProcess(List<Flux<Object>> predecessors, ProcessorState state,
        Integer referCount) {
        return predecessors.stream().map(predecessor -> {
            Flux<Object> flux = predecessor.doOnNext(x -> state.beforeProcess());
            return referCount == null ? flux.cache() : flux.publish().refCount(referCount);
        }).collect(Collectors.toList());
    }

    /**
     * 将数据入住到 @Parameter 标注的成员.
     */
    private IProcessor injectParam(IProcessor processor, ProcessorConfig config, Context context) {
        Config inner = config.getConfig();
        String id = (inner.hasPath("id")) ? inner.getString("id") : StringUtils.EMPTY;
        for (Tuple2<Field, Parameter> field : config.paramFields) {
            injectParam(processor, field.getT1(), field.getT2(), id, config, context);
        }
        return processor;
    }

    private void injectParam(IProcessor processor, Field field, Parameter parameter, String id,
        ProcessorConfig config, Context context) {
        String prefix = getParameterPrefix(parameter, id, processor);
        String key = getParameterPath(parameter, field);
        try {
            String str = getValue(config.getConfig(), context, prefix, key);
            Object value = parseValue(parameter, field, str);
            field.set(processor, value);
            if (log.isDebugEnabled()) {
                if (value instanceof ConfigValue) {
                    value = ((ConfigValue) value).unwrapped();
                }
                String message = String.format("%s=%s", field.getName(), value.toString());
                log.debug(message);
            }
        } catch (ConfigException.Missing e) {
            if (!parameter.nullable()) {
                String template = "missing injected parameter[%s] of \"%s[%s]\"";
                if (log.isInfoEnabled()) {
                    log.info(String.format(template, field.getType().getName(),
                        processor.getClass().getName(), field.getName()));
                }
            }
        } catch (Exception e) {
            String template = "inject parameter[%s] %s into \"%s[%s]\" failed";
            log.error(String.format(template, field.getType().getName(), key,
                processor.getClass().getName(), field.getName()), e);
        }
    }

    private String getValue(Config config, Context context, String prefix, String key) {
        String value = (String) context.requestParams().get(prefix + key);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        return config.getValue(key).render();
    }

    private Object parseValue(Parameter parameter, Field field, String value) {
        // parameter的name是类全称，则需要实例化
        if (parameter.isClassName()) {
            Class clazz = field.getType();
            boolean isContainer = parameter.initializeFromContainer();
            // parameter的name是类全称的Collection
            if (ClassUtils.isAssignable(clazz, Collection.class)) {
                Stream<Object> stream = ((Collection<String>) GsonUtil.jsonToObject(value, clazz)).stream()
                    .map(className -> newInstance(className, parameter.beanName(), isContainer))
                    .filter(Objects::nonNull);
                if (ClassUtils.isAssignable(clazz, List.class)) {
                    return stream.collect(Collectors.toList());
                }
                return stream.collect(Collectors.toSet());
            }
            return newInstance(StringUtils.strip(value, "\""), parameter.beanName(), isContainer);
        }
        // 转化parameter的值
        return GsonUtil.jsonToObject(value, field.getGenericType());
    }

    private Object newInstance(String className, String beanName, boolean isContainer) {
        try {
            Class clazz = Class.forName(className);
            if (isContainer) {
                if (StringUtils.isEmpty(beanName)) {
                    return beanContainer.getBean(clazz);
                }
                return beanContainer.getBean(beanName, clazz);
            }
            return Class.forName(className).newInstance();
        } catch (Exception e) {
            log.error("fail to instantiate '{}'", className);
            return null;
        }
    }

    private String getParameterPrefix(Parameter parameter, String id, IProcessor processor) {
        StringBuilder builder = new StringBuilder();
        if (parameter.idPrefix() && !id.isEmpty()) {
            builder.append(id).append(".");
        }
        if (parameter.simpleClassNamePrefix()) {
            builder.append(processor.getClass().getSimpleName()).append(".");
        }
        return builder.toString();
    }

    private String getParameterPath(
        Parameter parameter, Field field) {
        String name = parameter.name();
        if (name.isEmpty()) {
            name = field.getName();
        }
        return name;
    }
}
