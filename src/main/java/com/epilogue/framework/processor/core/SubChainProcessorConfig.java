package com.epilogue.framework.processor.core;

import static com.epilogue.framework.processor.util.Keys.MERGER;
import static com.epilogue.framework.processor.util.Keys.SELECTOR;
import static com.epilogue.framework.processor.util.Keys.SUB_CHAIN;

import com.google.gson.JsonObject;
import com.typesafe.config.Config;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
class SubChainProcessorConfig extends ProcessorConfig {

    /**
     * 判断执行哪些子chain的processor。
     */
    private final ProcessorConfig selectorConfig;
    /**
     * 并行执行的子chain配置，每个child对应一个子chain的执行流程
     */
    private final Map<String, ProcessorConfig> subChainConfigMap;
    /**
     * 待所有子chain执行完毕后，merge所有子chain的流程
     */
    private final ProcessorConfig mergerConfig;

    /**
     * parse {@link ProcessorConfig}.
     *
     * @param config config
     */
    SubChainProcessorConfig(Config config) {
        super(config);

        this.selectorConfig = ProcessorConfig.create(config.getConfig(SELECTOR));
        final Config subChains = config.getConfig(SUB_CHAIN);
        this.subChainConfigMap = config.getObject(SUB_CHAIN).keySet().stream()
            .collect(Collectors.toMap(
                key -> key,
                key -> ProcessorConfig.create(subChains.getConfig(key))
            ));
        this.mergerConfig = ProcessorConfig.create(config.getConfig(MERGER));
    }

    @Override
    public JsonObject toJsonObject(boolean simplified,
        Map<ProcessorConfig, ProcessorState> stateMap) {
        final JsonObject json = super.toJsonObject(simplified, stateMap);
        json.add(SELECTOR, selectorConfig.toJsonObject(simplified, stateMap));
        final JsonObject subChains = new JsonObject();
        ProcessorState state = stateMap.get(this);
        for (String selectorValue : state.selectorValues) {
            subChains.add(selectorValue, subChainConfigMap.get(selectorValue).toJsonObject(simplified, stateMap));
        }
        json.add(SUB_CHAIN, subChains);
        json.add(MERGER, mergerConfig.toJsonObject(simplified, stateMap));
        return json;
    }
}
