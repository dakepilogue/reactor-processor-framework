package com.emotibot.framework.processor.interfaces;

import com.emotibot.framework.processor.common.Context;

public interface ContextBuilder {
    Context build(Request request);
}
