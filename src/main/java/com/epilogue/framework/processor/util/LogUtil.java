package com.epilogue.framework.processor.util;

import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class LogUtil {

    /**
     * mdc put
     */
    public static void mdcFromContext(String key, Context context) {
        if (StringUtils.isNotEmpty(key) && context.hasKey(key)) {
            MDC.put(key, context.get(key));
        }
    }

    /**
     * 在MDC中记录uuid
     */
    public static <T> Function<? super T, Mono<T>> logUuid(String key) {
        return x -> Mono.subscriberContext().map(ctx -> {
            // 在新起的线程中先记录好uuid
            LogUtil.mdcFromContext(key, ctx);
            return x;
        });
    }

    /**
     * 在MDC中记录uuid
     */
    public static <T> Mono<T> logUuid(String key, Supplier<Mono<T>> monoSupplier) {
        return Mono.subscriberContext().doOnNext(ctx ->
            LogUtil.mdcFromContext(key, ctx)
        ).flatMap(x -> monoSupplier.get());
    }
}
