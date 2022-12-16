package com.epilogue.framework.processor.interfaces;

import com.epilogue.framework.processor.common.Context;

public interface ContextBuilder {

    /**
     * 根据request构造上下文对象
     */
    Context build(Request request);
}
