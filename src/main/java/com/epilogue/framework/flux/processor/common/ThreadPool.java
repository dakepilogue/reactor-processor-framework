package com.epilogue.framework.flux.processor.common;

import lombok.Getter;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * 框架内部用线程池
 */
@Getter
public class ThreadPool {

    /**
     * chain调度用线程池
     */
    private Scheduler scheduler;

    /**
     * chain记录超时用的线程池
     */
    private Scheduler timer;

    public ThreadPool(int threadNumber, String threadName) {
        this(Schedulers.newParallel(threadName, threadNumber));
    }

    /**
     * .
     */
    public ThreadPool(Scheduler s) {
        scheduler = s;
        timer = Schedulers.newParallel("processor-timer");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!scheduler.isDisposed()) {
                scheduler.dispose();
            }
            if (!timer.isDisposed()) {
                timer.dispose();
            }
        }));
    }
}
