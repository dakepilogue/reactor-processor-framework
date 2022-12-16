package com.epilogue.framework.processor.core;

import static com.epilogue.framework.processor.util.Keys.Latch.ALL;
import static com.epilogue.framework.processor.util.Keys.Latch.ANY;

import com.epilogue.framework.processor.annotation.Parameter;
import com.epilogue.framework.processor.common.ComposedContext;
import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.common.FrameworkConfig;
import com.epilogue.framework.processor.common.ProcessInfo;
import com.epilogue.framework.processor.common.WrappedContext;
import com.epilogue.framework.processor.interfaces.BeanContainer;
import com.epilogue.framework.processor.interfaces.Request;
import com.epilogue.framework.processor.util.GsonUtil;
import com.epilogue.framework.processor.util.LogUtil;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
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
    public Mono<Object> process() {
        Flux<Object> empty = frameworkConfig.isPublishOnNewThreadWhenStart()
            ? publishOnPool(Flux.empty()) : Flux.empty();
        return dispatch(empty, mainProcessorConfig, context)
            .flatMap(LogUtil.logUuid(frameworkConfig.getUuidKey()))
            .switchIfEmpty(Mono.create(sink -> {
                if (context.getThrowable() != null) {
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
    private Mono<Object> dispatch(Flux<Object> predecessors, ProcessorConfig config, WrappedContext context) {
        Class<? extends Processor> processorType = config.type;
        Mono<Object> preFuture;
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
                () -> Mono.fromRunnable(() ->
                    handleThrowable(throwable, config, context, isSingle)
                )
            )
        ).doOnError(throwable -> handleThrowable(throwable, config, context, isSingle));
    }

    private boolean isInErrors(Throwable throwable, Set<Class<? extends Throwable>> errorBlackList) {
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
            throwable = trimThrowable(throwable, context.getFrameworkConfig().getErrorStackTraceDepth());
            log.error(error, throwable);
            if (TimeoutException.class.isInstance(throwable)) {
                context.setThrowable(throwable);
            }
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
    private Mono<Object> singleDispatch(Flux<Object> predecessors,
        ProcessorConfig config, Class<? extends Processor> processorType, WrappedContext context) {
        // 将上一步processor的执行情况和返回结果封装在Try里面
        Flux<Try<Object>> transformed = predecessors.transform(transformer);
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        return mergePredecessors(transformed)
            .flatMap(pres -> {
                processorState.beforeProcess();
                Processor processor;
                if (config.beanName != null) {
                    processor = beanContainer.getBean(config.beanName, processorType);
                } else {
                    processor = beanContainer.getBean(processorType);
                }
                injectParam(processor, config, context);
                config.frameworkConfig = frameworkConfig;
                return (Mono<Object>) processor.processEntrance(context.getRealContext(), pres, config);
            })
            .doOnSuccess(x -> processorState.afterProcess())
            .doOnError(x -> processorState.afterProcess());
    }

    /**
     * 串行processor的调用逻辑
     */
    private Mono<Object> serialDispatch(Flux<Object> predecessors,
        SerialProcessorConfig config, WrappedContext context) {
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        // 将所有children节点链式地串起来
        for (int i = 0; i < config.getChildrenConfigs().size(); i++) {
            ProcessorConfig childConfig = config.getChildrenConfigs().get(i);
            Mono<Object> childFuture;
            if (i == 0) {
                childFuture = mergePredecessors(predecessors)
                    .doOnNext(x -> processorState.beforeProcess())
                    .flatMap(x -> dispatch(Flux.fromIterable(x), childConfig, context));
            } else {
                childFuture = dispatch(predecessors, childConfig, context);
            }
            if (i == config.getChildrenConfigs().size() - 1) {
                // 最后一个child要记录串行的结束时间
                childFuture = childFuture
                    .doOnSuccess(x -> processorState.afterProcess())
                    .doOnError(x -> processorState.afterProcess());
            }
            // 当前child的future为下一个child的前置
            predecessors = Flux.from(childFuture);
        }
        return predecessors.singleOrEmpty();
    }

    /**
     * 并行processor的调用逻辑
     */
    private Mono<Object> parallelDispatch(Flux<Object> predecessors,
        ParallelProcessorConfig config, WrappedContext context) {
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        List<ProcessorConfig> childrenConfigs = config.getChildrenConfigs();
        return mergePredecessors(predecessors)
            .doOnNext(x -> processorState.beforeProcess())
            .flatMap(list -> {
                Flux<Object> pre = Flux.fromIterable(list);
                List<Mono<Object>> monoList = Lists.newArrayListWithExpectedSize(childrenConfigs.size());
                // 第一个child的执行沿用上一个processor的线程
                monoList.add(dispatch(pre, childrenConfigs.get(0), context));
                // 其余child在新线程上执行
                for (int i = 1; i < childrenConfigs.size(); i++) {
                    monoList.add(dispatch(publishOnPool(pre), childrenConfigs.get(i), context));
                }
                String latch = StringUtils.isEmpty(config.getLatch()) ? ALL : config.getLatch().toLowerCase();
                Flux<Object> pres = parallelMerge(monoList, latch);
                // children并行之后执行merger
                return dispatch(pres, config.getMergerConfig(), context);
            })
            .doOnSuccess(x -> processorState.afterProcess())
            .doOnError(x -> processorState.afterProcess());
    }

    private <T> Flux<T> parallelMerge(List<Mono<T>> monoList, String latch) {
        if (latch.equals(ALL)) {
            return Flux.mergeSequential(monoList);
        } else if (latch.equals(ANY)) {
            return Flux.first(monoList);
        } else {
            try {
                int n = Integer.parseInt(latch);
                if (n >= monoList.size()) {
                    return Flux.error(new IndexOutOfBoundsException("latch:" + n));
                }
                Flux<T> mono = monoList.remove(n).flux().publish().refCount(2);
                Flux<T> pres = Flux.merge(monoList);
                return pres.takeUntilOther(mono.switchIfEmpty(Mono.never())).mergeWith(mono);
            } catch (NumberFormatException e) {
                return Flux.error(
                    new IllegalArgumentException("the value of latch can only be 'all', 'any' or number"));
            }
        }
    }

    /**
     * switch的调用逻辑
     */
    private Mono<Object> switchDispatch(Flux<Object> predecessors,
        SwitchProcessorConfig config, WrappedContext context) {
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        ProcessorConfig conditionProcessorConfig = config.getConditionConfig();
        // 先执行condition
        Mono<Tuple2<Object, List<Object>>> conditionMono = mergePredecessors(predecessors)
            .doOnNext(x -> processorState.beforeProcess())
            .flatMap(x -> Mono.zip(dispatch(Flux.fromIterable(x), conditionProcessorConfig, context), Mono.just(x)));
        return conditionMono.flatMap(condition -> {
            Object conditionValue = condition.getT1();
            if (conditionValue == null) {
                return Mono.error(new RuntimeException("found condition as \"null\" not String"));
            } else if (!String.class.isInstance(conditionValue)) {
                return Mono.error(new RuntimeException(String.format(
                    "found condition as \"%s\" not String",
                    conditionValue.getClass().getName())));
            }
            String key = (String) conditionValue;
            processorState.conditionValue = key;
            if (!config.getCasesConfigMap().containsKey(key)) {
                return Mono.error(new RuntimeException(String.format(
                    "can not found condition \"%s\"",
                    key)));
            }
            // 根据condition执行对应的case
            ProcessorConfig caseProcessorConfig = config.getCasesConfigMap().get(key);
            return dispatch(Flux.fromIterable(condition.getT2()), caseProcessorConfig, context);
        }).doOnSuccess(x -> processorState.afterProcess()).doOnError(x -> processorState.afterProcess());
    }

    /**
     * 批量请求的调用逻辑
     */
    private Mono<Object> composedContextParallelDispatch(Flux<Object> predecessors,
        ContextParallelProcessorConfig config, WrappedContext context) {
        if (!(context.getRealContext() instanceof ComposedContext)) {
            throw new RuntimeException(
                String.format("context is not a ComposedContext."));
        }
        ComposedContext<? extends Request> composedContext =
            (ComposedContext<? extends Request>) context.getRealContext();
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        return mergePredecessors(predecessors)
            .doOnNext(x -> processorState.beforeProcess())
            .flatMap(list -> {
                if (composedContext.getContexts().size() == 0) {
                    return Mono.empty();
                }
                Flux<Object> pre = Flux.fromIterable(list);
                List<Mono<Object>> monoList = Lists.newArrayListWithExpectedSize(composedContext.getContexts().size());
                // 每个子context执行一次chain config，并且是并行执行
                monoList.add(dispatch(pre, config.getChainConfig(),
                    WrappedContext.newContext(composedContext.getContexts().get(0), context)));
                for (int i = 1; i < composedContext.getContexts().size(); i++) {
                    monoList.add(dispatch(publishOnPool(pre), config.getChainConfig(),
                        WrappedContext.newContext(composedContext.getContexts().get(i), context)));
                }
                return mergePredecessors(Flux.mergeSequential(monoList)).map(x -> (Object) x);
            })
            .doOnSuccess(x -> processorState.afterProcess())
            .doOnError(x -> processorState.afterProcess());
    }

    /**
     * subChain的调用逻辑。
     * <p>
     * 根据selector返回的多个值，然后并行去执行subChain下面对应的多个子chain，最后再merge多个chain返回的结果
     * </p>
     */
    private Mono<Object> subChainDispatch(Flux<Object> predecessors,
        SubChainProcessorConfig config, WrappedContext context) {
        ProcessorState processorState = new ProcessorState();
        context.getStateMap().put(config, processorState);
        ProcessorConfig selectorProcessorConfig = config.getSelectorConfig();
        // 先执行selector
        Mono<Tuple2<Object, List<Object>>> selectorMono = mergePredecessors(predecessors)
            .doOnNext(x -> processorState.beforeProcess())
            .flatMap(x -> Mono.zip(dispatch(Flux.fromIterable(x), selectorProcessorConfig, context), Mono.just(x)));
        return selectorMono.flatMap(selector -> {
            Object selectorValue = selector.getT1();
            if (selectorValue == null) {
                return Mono.error(new RuntimeException("found selectorValue as \"null\" not String"));
            } else if (!Map.class.isInstance(selectorValue)) {
                return Mono.error(new RuntimeException(String.format(
                    "found selectorValue as \"%s\" not Map",
                    selectorValue.getClass().getName())));
            }
            Map<String, Object> values = (Map<String, Object>) selectorValue;
            processorState.selectorValues = ImmutableList.copyOf(values.keySet());
            Flux<Object> pre = Flux.fromIterable(selector.getT2());
            List<Mono<Object>> monoList = Lists.newArrayListWithExpectedSize(values.size());
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
                    monoList.add(subChainDispatch(pre, processorConfig, (Context) subChainContext, context, i));
                    i++;
                } else if (subChainContext instanceof List) {
                    List subChainContexts = (List) subChainContext;
                    for (Object chainContext : subChainContexts) {
                        if (chainContext instanceof Context) {
                            monoList.add(subChainDispatch(pre, processorConfig, (Context) chainContext, context, i));
                            i++;
                        }
                    }
                } else {
                    return Mono.error(new IllegalStateException(String.format(
                        "context returned from selector must be 'Context' or 'List', selectorValue is \"%s\"",
                        key)));
                }
            }
            // subChain并行之后执行merger
            return dispatch(Flux.mergeSequential(monoList), config.getMergerConfig(), context);
        }).doOnSuccess(x -> processorState.afterProcess()).doOnError(x -> processorState.afterProcess());
    }

    private Mono<Object> subChainDispatch(Flux<Object> pre, ProcessorConfig config, Context context,
        WrappedContext wrappedContext, int i) {
        WrappedContext subContext = WrappedContext.newContext(context, wrappedContext);
        if (i == 0) {
            // 第一个subChain的执行沿用上一个processor的线程
            return dispatch(pre, config, subContext);
        } else {
            // 其余subChain在新线程上执行
            return dispatch(publishOnPool(pre), config, subContext);
        }
    }

    private <T> Flux<T> publishOnPool(Flux<T> flux) {
        return flux.publishOn(pool) // 后续在新线程中执行
            .flatMap(LogUtil.logUuid(frameworkConfig.getUuidKey()));
    }

    /**
     * 合并上一步的processor，在它们都完成之后在执行后续逻辑
     */
    private <T> Mono<List<T>> mergePredecessors(Flux<T> predecessors) {
        return predecessors.collectList();
    }

    /**
     * 将数据入住到 @Parameter 标注的成员.
     */
    private Processor injectParam(Processor processor, ProcessorConfig config, Context context) {
        Config inner = config.getConfig();
        String id = (inner.hasPath("id")) ? inner.getString("id") : StringUtils.EMPTY;
        for (Tuple2<Field, Parameter> field : config.paramFields) {
            injectParam(processor, field.getT1(), field.getT2(), id, config, context);
        }
        return processor;
    }

    private void injectParam(Processor processor, Field field, Parameter parameter, String id,
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

    private String getParameterPrefix(Parameter parameter, String id, Processor processor) {
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
