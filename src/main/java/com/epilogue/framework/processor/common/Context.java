package com.epilogue.framework.processor.common;

import com.epilogue.framework.processor.basic.Filter;
import com.epilogue.framework.processor.basic.Ruler;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;

public interface Context<T> {

    /**
     * 从context中获取数据。 用于 {@link Filter} 和 {@link Ruler}
     */
    default Collection<T> getItemList() {
        return ImmutableList.of();
    }

    /**
     * 设置context中的数据。 用于 {@link Filter} 和 {@link Ruler}
     */
    default void setItemList(Collection<T> list) {
    }

    /**
     * 用于覆盖processor参数
     */
    default Map<String, String> requestParams() {
        return ImmutableMap.of();
    }
}
