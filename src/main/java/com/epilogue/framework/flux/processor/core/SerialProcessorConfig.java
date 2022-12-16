package com.epilogue.framework.flux.processor.core;

import static com.epilogue.framework.flux.processor.util.Keys.CHILDREN;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.typesafe.config.Config;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
class SerialProcessorConfig extends ProcessorConfig {

    /**
     * 串行执行的子chain配置，每个child对应一个子chain的执行流程
     */
    private final List<ProcessorConfig> childrenConfigs;

    /**
     * parse {@link ProcessorConfig}.
     *
     * @param config config
     */
    SerialProcessorConfig(Config config) {
        super(config);

        this.childrenConfigs = config.getConfigList(CHILDREN).stream()
            .map(c -> create(c))
            .collect(Collectors.toList());
    }

    public List<ProcessorConfig> getChildrenConfigs() {
        return childrenConfigs;
    }

    @Override
    public JsonObject toJsonObject(boolean simplified,
        Map<ProcessorConfig, ProcessorState> stateMap) {
        final JsonObject json = super.toJsonObject(simplified, stateMap);
        final JsonArray childrenArray = new JsonArray();
        for (ProcessorConfig child : childrenConfigs) {
            childrenArray.add(child.toJsonObject(simplified, stateMap));
        }
        json.add(CHILDREN, childrenArray);
        return json;
    }
}
