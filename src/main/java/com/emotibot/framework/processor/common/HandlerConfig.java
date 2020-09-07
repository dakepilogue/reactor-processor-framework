package com.emotibot.framework.processor.common;

import com.typesafe.config.Config;
import lombok.Getter;

@Getter
public class HandlerConfig {

    private Config rawConfig;

    /**
     * .
     */
    public HandlerConfig(Config config) {
        this.rawConfig = config;
    }
}
