package com.epilogue.framework.flux.processor.core;

import static com.epilogue.framework.flux.processor.util.Keys.CHILDREN;
import static com.epilogue.framework.flux.processor.util.Keys.LATCH;
import static com.epilogue.framework.flux.processor.util.Keys.MERGER;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.typesafe.config.Config;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
class ParallelProcessorConfig extends ProcessorConfig {

    /**
     * 并行执行的子chain配置，每个child对应一个子chain的执行流程
     */
    private final List<ProcessorConfig> childrenConfigs;
    /**
     * 待所有子chain执行完毕后，merge所有子chain的流程
     */
    private final ProcessorConfig mergerConfig;
    /**
     * 并行等待的条件。 <br>
     * ALL: 等待所有child执行完毕 <br>
     * ANY: 任意child执行完毕 <br>
     * 0/1/2/.../n-1: 等待第n个child执行完毕且数据不为empty，否则fall back到ALL <br>
     */
    private String latch;

    /**
     * parse {@link ProcessorConfig}.
     *
     * @param config config
     */
    ParallelProcessorConfig(Config config) {
        super(config);

        this.childrenConfigs = config.getConfigList(CHILDREN).stream()
            .map(c -> create(c))
            .collect(Collectors.toList());
        this.mergerConfig = create(config.getConfig(MERGER));
        this.latch = config.hasPath(LATCH) ? config.getString(LATCH) : null;
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
        json.add(MERGER, mergerConfig.toJsonObject(simplified, stateMap));
        return json;
    }
}
