package com.epilogue.framework.flux.processor.util;

public interface Keys {

    String CLASS = "class";
    String BEAN = "bean";
    String TIMEOUT = "timeout";
    String LATENCY = "latency";
    String CHILDREN = "children";
    String CHAIN = "chain";
    String MERGER = "merger";
    String LATCH = "latch";
    String CASES = "cases";
    String CONDITION = "condition";
    String CONDITION_VALUE = "conditionValue";
    String SUB_CHAIN = "subChain";
    String SELECTOR = "selector";
    String WINDOWS = "windows";

    interface Latch {
        String ALL = "all";
        String ANY = "any";
    }
}
