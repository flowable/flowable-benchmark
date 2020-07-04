package org.flowable.benchmark.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "benchmark")
public class BenchmarkProperties {

    protected boolean enabled = true;

    protected int minThreads = 10;
    protected int maxThreads = 16;
    protected int iterations = 10000;

    protected boolean treeFetch = true;

    protected List<String> processes = new ArrayList<>();

    protected String outputName = "results.csv";

    protected final AsyncHistory asyncHistory = new AsyncHistory();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public boolean isTreeFetch() {
        return treeFetch;
    }

    public void setTreeFetch(boolean treeFetch) {
        this.treeFetch = treeFetch;
    }

    public List<String> getProcesses() {
        return processes;
    }

    public void setProcesses(List<String> processes) {
        this.processes = processes;
    }

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public AsyncHistory getAsyncHistory() {
        return asyncHistory;
    }

    public static class AsyncHistory {

        protected boolean enabled;
        protected boolean grouping;
        protected boolean gzip;
        protected int groupingThreshold = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isGrouping() {
            return grouping;
        }

        public void setGrouping(boolean grouping) {
            this.grouping = grouping;
        }

        public boolean isGzip() {
            return gzip;
        }

        public void setGzip(boolean gzip) {
            this.gzip = gzip;
        }

        public int getGroupingThreshold() {
            return groupingThreshold;
        }

        public void setGroupingThreshold(int groupingThreshold) {
            this.groupingThreshold = groupingThreshold;
        }
    }
}
