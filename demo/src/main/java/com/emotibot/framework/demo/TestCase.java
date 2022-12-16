package com.epilogue.framework.demo;

import static com.epilogue.framework.demo.Constants.UUID;

import com.epilogue.framework.demo.core.DelayedDebugStub;
import com.epilogue.framework.flux.processor.ProcessorFramework;
import com.epilogue.framework.flux.processor.common.Context;
import com.epilogue.framework.flux.processor.common.FrameworkConfig;
import com.epilogue.framework.flux.processor.common.ProcessInfo;
import com.epilogue.framework.flux.processor.interfaces.Request;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.util.function.Tuple2;

@Slf4j
public class TestCase {

    public static void main(String[] args) {
        // 使用默认的BeanContainer和线程数构造ProcessorFramework
        ProcessorFramework processorFramework = new ProcessorFramework(request -> new TestContext());
        // 配置参数
        processorFramework.setFrameworkConfig(FrameworkConfig.builder()
            .publishOnNewThreadWhenStart(true)  // 第一个Processor是否在新线程中运行，默认为false（沿用调用process接口所在的线程）
            .simplifiedTimeInfo(true)           // 简化输出的TimeInfo
            .errorStackTraceDepth(3)            // 减少打印的错误堆栈信息
            .build());
        // 获取dummy的配置
        Config rawConfig = ConfigFactory.parseResources("handlers.conf").resolve();
        Config dummyHandler = rawConfig.getConfig("handlers").getConfig("dummy");
        // 调用process接口
        processorFramework.process(new TestRequest(), dummyHandler)
            .doOnNext(x -> log.info(x.toString()))
            .collectList()
            .block();
        // 调用processWithInfo接口
        List<Tuple2<Object, ProcessInfo>> tuple2 = processorFramework
            .processWithInfo(new TestRequest(), dummyHandler)
            .doOnNext(x -> log.info(x.toString()))
            .collectList()
            .subscriberContext(reactor.util.context.Context.of(UUID, "test_uuid"))  // 传递UUID，方便在logback日志中打印（引用方式：%X{your_key}）
            .block();
        log.info(tuple2.get(0).getT2().getTimeInfo().toString());
        processorFramework.shutDown();
        DelayedDebugStub.shutDown();
    }


    public static class TestRequest implements Request {

    }

    public static class TestContext implements Context {

    }
}
