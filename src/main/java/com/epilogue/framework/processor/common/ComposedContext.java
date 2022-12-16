package com.epilogue.framework.processor.common;

import java.util.List;

public interface ComposedContext<C extends ComposedContext<C>> extends Context {

    List<? extends Context> getContexts();
}
