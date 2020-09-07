package com.emotibot.framework.processor.common;

import com.typesafe.config.Config;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * ActionConfig. contains all info that create a processor needs. actionConfig usually is a part of
 * SystemConfig.config.
 */
public class ProcessorConfig {

    public final String beanName;
    public final String className;
    public final List<ProcessorConfig> childrenConfigs;
    public final ProcessorConfig mergerConfig;
    public final Long timeout;
    private final Config config;

    /**
     * parse {@link ProcessorConfig}.
     *
     * @param config config
     */
    public ProcessorConfig(Config config) {
        this.config = config;
        this.className = config.getString("class");
        String[] str = this.className.split("\\.");
        beanName = StringUtils.uncapitalize(str[str.length - 1]);
        if (config.hasPath("children")) {
            this.childrenConfigs = config.getConfigList("children").stream()
                .map(ProcessorConfig::new).collect(Collectors.toList());
        } else {
            this.childrenConfigs = null;
        }
        if (config.hasPath("merger")) {
            this.mergerConfig = new ProcessorConfig(config.getConfig("merger"));
        } else {
            this.mergerConfig = null;
        }
        if (config.hasPath("timeout")) {
            this.timeout = config.getDuration("timeout", TimeUnit.MILLISECONDS);
        } else {
            this.timeout = null;
        }
    }

    public Config getConfig() {
        return config;
    }
}
