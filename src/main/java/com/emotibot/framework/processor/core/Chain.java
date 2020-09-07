package com.emotibot.framework.processor.core;

import com.emotibot.framework.processor.annotation.Parameter;
import com.emotibot.framework.processor.common.ComposedContext;
import com.emotibot.framework.processor.common.Context;
import com.emotibot.framework.processor.common.ProcessorConfig;
import com.emotibot.framework.processor.interfaces.BeanContainer;
import com.emotibot.framework.processor.interfaces.Request;
import com.emotibot.framework.processor.util.GsonUtil;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Slf4j
public class Chain {

    private final ProcessorConfig mainProcessorConfig;
    private final Context context;
    private final Scheduler pool;
    private final Scheduler timer;
    private final BeanContainer beanContainer;
    private final Function<Mono<Object>, Mono<Try<Object>>> transformer;

    /**
     * .
     */
    public Chain(ProcessorConfig mainProcessorConfig, Context context, BeanContainer beanContainer) {
        this.context = context;
        this.mainProcessorConfig = mainProcessorConfig;
        this.beanContainer = beanContainer;
        this.pool = beanContainer.getBean("logic", Scheduler.class);
        this.timer = beanContainer.getBean("chain", Scheduler.class);
        transformer = mono -> mono.onErrorResume(throwable -> Mono.just(new Try<>(throwable)))
            .map(x -> x instanceof Try ? (Try<Object>) x : new Try<>(x));
    }

    private static Class<? extends Processor> getProcessorType(String className) {
        Class<? extends Processor> processorType;
        try {
            Class type = Class.forName(className);
            if (!Processor.class.isAssignableFrom(type)) {
                throw new RuntimeException(
                    String.format("class \"%s\" is not a Processor.", className));
            }
            processorType = (Class<? extends Processor>) type;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("class \"%s\" not fond.", className), e);
        }
        return processorType;
    }

    public Mono<Object> process() {
        return dispatch(Collections.singletonList(Mono.just((Object) StringUtils.EMPTY).publishOn(pool)),
            mainProcessorConfig, context);
    }

    private Mono<Object> dispatch(List<Mono<Object>> predecessors, ProcessorConfig config, Context context) {
        Class<? extends Processor> processorType = getProcessorType(config.className);
        Mono<Object> preFuture;
        if (Parallel.class.isAssignableFrom(processorType)) {
            preFuture = parallelDispatch(predecessors, config, context);
        } else if (Serial.class.isAssignableFrom(processorType)) {
            preFuture = serialDispatch(predecessors, config, context);
        } else if (ComposedContextParallel.class.isAssignableFrom(processorType)) {
            preFuture = composedContextParallelDispatch(predecessors, config, context);
        } else {
            preFuture = singleDispatch(predecessors, config, processorType, context);
        }
        return preFuture.timeout(Duration.ofMillis(config.timeout), timer)
            .doOnError(throwable -> {
                if (!PredecessorException.class.isInstance(throwable)) {
                    log.error(String.format(
                        "\"%s\" fail, %s",
                        config.className,
                        config.getConfig().origin().toString()
                    ), throwable);
                }
            });
    }

    private Mono<Object> singleDispatch(List<Mono<Object>> predecessors,
        ProcessorConfig config, Class<? extends Processor> processorType, Context context) {
        Processor processor = beanContainer.getBean(config.beanName, processorType);
        injectParam(processor, config);
        List<Mono<Try<Object>>> transformed = predecessors.stream()
            .map(predecessor -> predecessor.transform(transformer))
            .collect(Collectors.toList());
        return Mono.zip(transformed, Lists::newArrayList)
            .flatMap(pres -> processor.processEntrance(context, pres, config));
    }

    private Mono<Object> serialDispatch(List<Mono<Object>> predecessors,
        ProcessorConfig config, Context context) {
        for (ProcessorConfig childConfig : config.childrenConfigs) {
            Mono<Object> childFuture = dispatch(predecessors, childConfig, context);
            predecessors = Collections.singletonList(childFuture);
        }
        return predecessors.get(0);
    }

    private Mono<Object> parallelDispatch(List<Mono<Object>> predecessors,
        ProcessorConfig config, Context context) {
        return Mono.zip(predecessors, Lists::newArrayList).flatMap(list -> {
                Mono<Object> pre = Mono.just(list);
                List<Mono<Object>> monoList = Lists.newArrayListWithExpectedSize(config.childrenConfigs.size());
                monoList.add(dispatch(Collections.singletonList(pre), config.childrenConfigs.get(0), context));
                for (int i = 1; i < config.childrenConfigs.size(); i++) {
                    monoList.add(dispatch(Collections.singletonList(pre.publishOn(pool)),
                        config.childrenConfigs.get(i), context));
                }
                return dispatch(monoList, config.mergerConfig, context);
            }
        );
    }

    private Mono<Object> composedContextParallelDispatch(List<Mono<Object>> predecessors,
        ProcessorConfig config, Context context) {
        if (!(context instanceof ComposedContext)) {
            throw new RuntimeException(
                String.format("context is not a ComposedContext."));
        }
        ComposedContext<? extends Request> composedContext = (ComposedContext<? extends Request>) context;
        return Mono.zip(composedContext.getContexts().stream().map(
            ctx -> dispatch(predecessors, config.mergerConfig, ctx)
        ).collect(Collectors.toList()), Lists::newArrayList).map(Function.identity());
    }

    /**
     * 将数据入住到 @Parameter 标注的成员.
     */
    private Processor injectParam(Processor processor, ProcessorConfig config) {
        Class<? extends Processor> processorType = processor.getClass();
        Config inner = config.getConfig();
        String id = (inner.hasPath("id")) ? inner.getString("id") : StringUtils.EMPTY;
        for (Field field : FieldUtils.getAllFieldsList(processorType)) {
            injectParam(processor, field, id, config);
        }
        return processor;
    }

    private void injectParam(Processor processor, Field field, String id,
        ProcessorConfig config) {
        Parameter parameter = field.getAnnotation(Parameter.class);
        if (parameter == null) {
            return;
        }
        String prefix = getParameterPrefix(parameter, id, processor);
        String key = getParameterPath(parameter, field);
        try {
            Object value = getValue(parameter, field, config.getConfig(), key);
            field.setAccessible(true);
            field.set(processor, value);
            if (log.isDebugEnabled()) {
                if (value instanceof ConfigValue) {
                    value = ((ConfigValue) value).unwrapped();
                }
                String message = String.format("%s=%s", field.getName(), value.toString());
                log.debug(message);
            }
        } catch (Exception e) {
            String template = "inject parameter[%s] %s into \"%s[%s]\" failed";
            log.error(String.format(template, field.getType().getName(), key,
                processor.getClass().getName(), field.getName()), e);
        }
    }

    private Object getValue(Parameter parameter, Field field, Config config, String key) {
        if (parameter.isClassName()) {
            Class clazz = field.getType();
            if (ClassUtils.isAssignable(clazz, Collection.class)) {
                String str = config.getValue(key).render();
                Stream<Object> stream = ((Collection<String>) GsonUtil.jsonToObject(str, clazz)).stream()
                    .map(this::newInstance)
                    .filter(Objects::nonNull);
                if (ClassUtils.isAssignable(clazz, List.class)) {
                    return stream.collect(Collectors.toList());
                }
                return stream.collect(Collectors.toSet());
            }
            return newInstance(config.getString(key));
        }
        if (config.getIsNull(key)) {
            return null;
        }
        String str = config.getValue(key).render();
        return GsonUtil.jsonToObject(str, field.getGenericType());
    }

    private Object newInstance(String className) {
        try {
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
