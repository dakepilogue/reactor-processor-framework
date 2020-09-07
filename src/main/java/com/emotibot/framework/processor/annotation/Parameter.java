package com.emotibot.framework.processor.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Inherited
@Target({FIELD})
@Retention(RUNTIME)
@Documented
public @interface Parameter {

    /**
     * 使用指定的 name.
     */
    String name() default "";

    /**
     * 使用类名作为前缀.
     */
    boolean simpleClassNamePrefix() default true;

    /**
     * 如果设置了 id 则使用 id 作为前缀.
     */
    boolean idPrefix() default true;

    /**
     * 是否是类名，如果是则实例化后再注入.
     */
    boolean isClassName() default false;
}
