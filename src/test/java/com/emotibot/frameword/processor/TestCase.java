package com.emotibot.frameword.processor;

import com.emotibot.framework.processor.ProcessorFramework;
import com.emotibot.framework.processor.common.Context;
import com.emotibot.framework.processor.interfaces.Request;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class TestCase {

    @Test
    public void test() {
        Config rawConfig = ConfigFactory.parseResources(this.getClass(), "/processor.conf").resolve();
        ProcessorFramework processorFramework = new ProcessorFramework(request -> new TestContext());
        processorFramework.process(new TestRequest(), rawConfig.getConfig("handlers").getConfig("dummy"))
            .doOnNext(x -> log.info(x.toString()))
            .block();
    }


    public class TestContext implements Context {

    }

    public class TestRequest implements Request {

    }
}
