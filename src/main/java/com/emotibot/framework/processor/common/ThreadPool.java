package com.emotibot.framework.processor.common;

import lombok.Getter;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Getter
public class ThreadPool {

    private Scheduler scheduler;
    private Scheduler timer;

    /**
     * .
     */
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
