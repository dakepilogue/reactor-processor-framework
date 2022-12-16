package com.epilogue.framework.flux.processor.core;

import static com.epilogue.framework.flux.processor.util.Keys.CASES;
import static com.epilogue.framework.flux.processor.util.Keys.CONDITION;
import static com.epilogue.framework.flux.processor.util.Keys.CONDITION_VALUE;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.typesafe.config.Config;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

class SwitchProcessorConfig extends ProcessorConfig {

    /**
     * 生成条件的processor。
     */
    private final ProcessorConfig conditionConfig;
    /**
     * 不同条件对应的case，每个case对应一个执行流程。
     * <p>
     * 具体执行的时候会根据conditionConfig生成的条件执行对应的一个case
     * </p>
     */
    private final Map<String, ProcessorConfig> casesConfigMap;

    /**
     * parse {@link ProcessorConfig}.
     *
     * @param config config
     */
    SwitchProcessorConfig(Config config) {
        super(config);

        this.conditionConfig = create(config.getConfig(CONDITION));
        final Config cases = config.getConfig(CASES);
        this.casesConfigMap = config.getObject(CASES).keySet().stream()
            .collect(Collectors.toMap(
                key -> key,
                key -> create(cases.getConfig(key))
            ));
    }

    public ProcessorConfig getConditionConfig() {
        return conditionConfig;
    }

    public Map<String, ProcessorConfig> getCasesConfigMap() {
        return casesConfigMap;
    }

    @Override
    public JsonObject toJsonObject(boolean simplified,
        Map<ProcessorConfig, ProcessorState> stateMap) {
        final JsonObject json = super.toJsonObject(simplified, stateMap);
        final JsonObject cases = new JsonObject();
        ProcessorState state = stateMap.get(this);
        for (Entry<String, ProcessorConfig> entry : casesConfigMap.entrySet()) {
            if (entry.getKey().equals(state.conditionValue)) {
                cases.add(entry.getKey(), entry.getValue().toJsonObject(simplified, stateMap));
            }
        }
        json.add(CASES, cases);
        json.add(CONDITION, conditionConfig.toJsonObject(simplified, stateMap));
        if (!simplified && state.conditionValue != null) {
            json.add(CONDITION_VALUE, new JsonPrimitive(state.conditionValue));
        }
        return json;
    }
}
