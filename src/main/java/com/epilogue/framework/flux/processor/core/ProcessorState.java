package com.epilogue.framework.flux.processor.core;

import com.google.gson.annotations.Expose;
import java.util.List;
import java.util.Optional;

public class ProcessorState {

    @Expose(serialize = false)
    private volatile long beginTick = -1;
    @Expose(serialize = false)
    private volatile long endTick = -1;
    @Expose(serialize = false)
    private volatile long latency = -1;
    String conditionValue = null;
    List<String> selectorValues = null;

    public Optional<Long> getBeginTick() {
        return beginTick < 0 ? Optional.empty() : Optional.of(beginTick);
    }

    public Optional<Long> getEndTick() {
        return endTick < 0 ? Optional.empty() : Optional.of(endTick);
    }

    /**
     * 获取processor执行完成的延迟
     */
    public Optional<Long> getLatency() {
        if (getBeginTick().isPresent() && getEndTick().isPresent()) {
            latency = endTick - beginTick;
        }
        return latency < 0 ? Optional.empty() : Optional.of(latency);
    }

    /**
     * 在当前{@link Processor}调用前调用. 用于记录 {@link #beginTick}, {@link #endTick},
     * {@link #latency}
     */
    void beforeProcess() {
        if (!getBeginTick().isPresent()) {
            beginTick = System.currentTimeMillis();
        }
    }

    /**
     * 在当前{@link Processor}调用后调用. 用于记录 {@link #beginTick}, {@link #endTick},
     * {@link #latency}
     */
    void afterProcess() {
        endTick = System.currentTimeMillis();
    }
}
