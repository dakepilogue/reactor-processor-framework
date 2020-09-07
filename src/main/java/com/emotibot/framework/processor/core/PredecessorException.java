package com.emotibot.framework.processor.core;

import com.google.common.base.Throwables;
import java.util.Arrays;
import java.util.Collections;

/**
 * 表示当前 Processor 因为之前的 Processor 没有正确返回结果，发生的异常. 之前的 Processor 的错误将被包含在 causes 中
 */
public class PredecessorException extends RuntimeException {

    private final Throwable[] causes;
    private final Class<? extends Processor> processor;

    /**
     * 使用完整的参数创建.
     */
    public PredecessorException(String message, Throwable[] causes,
        Class<? extends Processor> processor) {
        super(message, causes[0]);
        this.causes = causes;
        this.processor = processor;
    }

    /**
     * 使用默认 message 创建.
     *
     * @param causes predecessor 中的错误
     */
    public PredecessorException(Throwable[] causes, Class<? extends Processor> processor) {
        super(String.format(
            "%s fail for %d predecessor fail.",
            processor.getName(),
            causes.length
        ), causes[0]);
        this.causes = causes;
        this.processor = processor;
    }

    private static StringBuilder buildErrorStackString(Throwable error, StringBuilder builder) {
        return buildErrorStackString(error, builder, 0);
    }

    private static StringBuilder buildErrorStackString(Throwable error, StringBuilder builder,
        int indent) {
        if (!PredecessorException.class.isInstance(error)) {
            builder.append(Throwables.getStackTraceAsString(error));
            return builder;
        }
        PredecessorException exception = (PredecessorException) error;
        builder.append(String.join("", Collections.nCopies(indent, "  ")));
        builder.append(String.format("\"%s\" fail caused by %s:\n",
            ((PredecessorException) error).getProcessor().getName(), error.getMessage()));
        for (Throwable throwable : exception.getCauses()) {
            buildErrorStackString(throwable, builder, indent + 1);
        }
        return builder;
    }

    public Throwable[] getCauses() {
        return causes;
    }

    public Class<? extends Processor> getProcessor() {
        return processor;
    }

    /**
     * 返回错误的详细 Processor 调用栈.
     *
     * @return 多行的调用信息
     */
    public String logErrorStackToString() {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(causes).forEach(e -> buildErrorStackString(e, builder));
        return builder.toString();
    }
}
