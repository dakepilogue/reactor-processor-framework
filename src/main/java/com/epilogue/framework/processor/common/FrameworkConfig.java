package com.epilogue.framework.processor.common;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 框架相关配置
 */
@Getter
@Builder
@AllArgsConstructor
public class FrameworkConfig {

    public static final FrameworkConfig DEFAULT;
    public static final Config COMMON_CONFIG;
    private static final String DEFAULT_UUID = "uuid";

    static {
        DEFAULT = FrameworkConfig.builder().build();
        COMMON_CONFIG = ConfigFactory.parseResources(FrameworkConfig.class, "/common.conf");
    }

    /**
     * true：如果chain中某个processor节点抛异常，则丢弃并打印异常，然后继续执行。false：一旦出现异常则退出chain。
     */
    private boolean onErrorContinue;

    /**
     * 异常错误的黑名单，当onErrorContinue=true时生效。 该黑名单中的异常不会产生onErrorContinue的效果。
     */
    private Set<Class<? extends Throwable>> errorBlackList;

    /**
     * chain一开始在新线程上执行
     */
    private boolean publishOnNewThreadWhenStart;

    /**
     * 简化processor的耗时信息
     */
    private boolean simplifiedTimeInfo;

    /**
     * 上下文中传递请求ID的key名称
     */
    private String uuidKey;

    /**
     * 打印Processor错误日志的堆栈深度，0为所有
     */
    private int errorStackTraceDepth;

    public String getUuidKey() {
        return StringUtils.isEmpty(uuidKey) ? DEFAULT_UUID : uuidKey;
    }
}
