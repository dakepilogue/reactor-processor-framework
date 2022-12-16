package com.epilogue.framework.processor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.epilogue.framework.processor.common.Context;
import com.epilogue.framework.processor.common.FrameworkConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class CaseTest {

    private ProcessorFramework processorFramework;
    private Config handlerConfig;

    @Before
    public void setup() {
        processorFramework = new ProcessorFramework(null);
        // 配置参数
        processorFramework.setFrameworkConfig(FrameworkConfig.builder()
            .publishOnNewThreadWhenStart(true)  // 第一个Processor是否在新线程中运行，默认为false（沿用调用process接口所在的线程）
            .simplifiedTimeInfo(true)           // 简化输出的TimeInfo
            .errorStackTraceDepth(3)            // 减少打印的错误堆栈信息
            .onErrorContinue(true)
            .errorBlackList(Sets.newHashSet(IllegalArgumentException.class))
            .build());
        // 获取dummy的配置
        handlerConfig = ConfigFactory.parseResources("handlers.conf").resolve().getConfig("handlers");
    }

    @Test
    public void parallelLatchTest() {
        Config handler = handlerConfig.getConfig("parallelLatch");
        // 调用process接口
        processorFramework.process(new TestContext(), handler).block();
    }

    @Test
    public void switchTest() {
        Config handler = handlerConfig.getConfig("switchTest");
        // 调用process接口
        processorFramework.process(new TestContext(), handler).block();
    }

    @Test
    public void paramTest() {
        Config handler = handlerConfig.getConfig("paramsTest");
        MockItem mockItem = mock(MockItem.class);
        processorFramework.getBeanContainer().registerBean("mockItem", MockItem.class, mockItem);
        // 调用process接口
        processorFramework.process(new TestContext(), handler).block();
        verify(mockItem, times(3)).invoke();
    }

    @Test
    public void onErrorContinueTest() {
        Config handler = handlerConfig.getConfig("onErrorContinueTest");
        MockItem mockItem = mock(MockItem.class);
        processorFramework.getBeanContainer().registerBean("mockItem", MockItem.class, mockItem);
        boolean hasException = false;
        // 调用process接口
        try {
            processorFramework.process(new TestContext(), handler).block();
        } catch (Exception e) {
            hasException = true;
        }
        assert hasException;
        verify(mockItem, times(1)).invoke();
    }

    public static class TestContext implements Context {

        @Override
        public Map<String, String> requestParams() {
            return ImmutableMap.of("test.DebugStub.msg", "\"sleep 200ms\"",
                "testParameter.msg", "\"testParameter\"");
        }
    }

}
