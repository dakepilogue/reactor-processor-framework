package com.epilogue.framework.flux.processor;

import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.common.FrameworkConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import reactor.core.publisher.Flux;

public class CaseTest {

    public void practice() {
        ProcessorFramework processorFramework = new ProcessorFramework(null);
        // 配置参数
        processorFramework.setFrameworkConfig(FrameworkConfig.builder()
            .publishOnNewThreadWhenStart(true)  // 第一个Processor是否在新线程中运行，默认为false（沿用调用process接口所在的线程）
            .simplifiedTimeInfo(true)           // 简化输出的TimeInfo
            .errorStackTraceDepth(3)            // 减少打印的错误堆栈信息
            .onErrorContinue(true)              // 忽略异常错误，不中断执行
            .build());
        // 获取practice的配置
        Config handlerConfig = ConfigFactory.parseResources("handler_practice.conf").resolve().getConfig("practice");
        // 调用process接口
        Flux<Object> flux = processorFramework.process(new TestContext(), handlerConfig);
        // ...后续处理
    }

    public static class TestContext implements Context {

    }
}
