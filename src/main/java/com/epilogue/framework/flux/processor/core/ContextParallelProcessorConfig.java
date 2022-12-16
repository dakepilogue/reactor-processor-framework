package com.epilogue.framework.flux.processor.core;

import static com.epilogue.framework.flux.processor.util.Keys.CHAIN;

import com.google.gson.JsonObject;
import com.typesafe.config.Config;
import java.util.Map;
import lombok.Getter;

@Getter
class ContextParallelProcessorConfig extends ProcessorConfig {

    private final ProcessorConfig chainConfig;

    /**
     * parse {@link ProcessorConfig}.
     *
     * @param config config
     */
    ContextParallelProcessorConfig(Config config) {
        super(config);
        this.chainConfig = create(config.getConfig(CHAIN));
    }

    @Override
    public JsonObject toJsonObject(boolean simplified,
        Map<ProcessorConfig, ProcessorState> stateMap) {
        final JsonObject json = super.toJsonObject(simplified, stateMap);
        json.add(CHAIN, chainConfig.toJsonObject(simplified, stateMap));
        return json;
    }
}
