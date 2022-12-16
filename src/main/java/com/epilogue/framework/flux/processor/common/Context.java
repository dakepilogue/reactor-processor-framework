package com.epilogue.framework.flux.processor.common;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public interface Context<T> {

    /**
     * 用于覆盖processor参数
     */
    default Map<String, String> requestParams() {
        return ImmutableMap.of();
    }

    /**
     * 返回每个subChain的子chain数量
     */
    default Map<String, Integer> subChainCount() {
        return ImmutableMap.of();
    }

    /**
     * 获取subchain流合并到merger的时候，对应的index，从0开始
     */
    default Integer subChainIndex() {
        return null;
    }
}
