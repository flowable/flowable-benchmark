package org.flowable.benchmark.app.runnable;

import java.util.concurrent.Callable;

/**
 * @author Filip Hrisafov
 */
public abstract class BenchmarkRunnable implements Callable<Long> {

    @Override
    public Long call() {
        long startTime = System.currentTimeMillis();
        executeRun();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    protected abstract void executeRun();

    public abstract String getDescription();
}
