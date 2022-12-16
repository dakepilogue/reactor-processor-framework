package com.epilogue.framework.processor.common;

import com.google.gson.JsonObject;
import lombok.Getter;

/**
 * chain单次请求执行情况的相关信息
 */
@Getter
public class ProcessInfo {

    private final JsonObject timeInfo;

    public ProcessInfo(JsonObject timeInfo) {
        this.timeInfo = timeInfo;
    }
}
