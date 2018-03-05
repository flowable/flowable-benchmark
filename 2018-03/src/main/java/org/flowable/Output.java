/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Precision;
import org.flowable.runnable.BenchmarkRunnable;

public class Output {

    protected String outputFileName;
    protected int maxNrOfThreads;
    protected int nrOfIterations;
    protected List<ProcessOutput> processOutputs = new ArrayList<>();
    
    public Output(String outputFileName, int maxNrOfThreads, int nrOfIterations) {
        this.outputFileName = outputFileName;
        this.maxNrOfThreads = maxNrOfThreads;
        this.nrOfIterations = nrOfIterations;
    }
    
    public void addResults(String process, int nrOfThreads, int nrOfIterations, long totalDurationInMs, List<BenchmarkRunnable> benchmarkRunnables) {
        
        double[] timings = new double[benchmarkRunnables.size()];
        for (int i = 0; i < benchmarkRunnables.size(); i++) {
            timings[i] = benchmarkRunnables.get(i).getEndTime() - benchmarkRunnables.get(i).getStartTime();
        }
        
        ProcessOutput processOutput = getProcessOutput(process);
        if (processOutput == null) {
            processOutput = new ProcessOutput(process, maxNrOfThreads);
            processOutputs.add(processOutput);
        }
        
        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics(timings);
        processOutput.addMeasurement(nrOfThreads, 
                descriptiveStatistics.getMean(), 
                descriptiveStatistics.getStandardDeviation(), 
                calculateThroughputPerSecond(totalDurationInMs, nrOfIterations));
    }
    
    protected ProcessOutput getProcessOutput(String process) {
        for (ProcessOutput processOutput : processOutputs) {
            if (processOutput.getName().equals(process)) {
                return processOutput;
            }
        }
        return null;
    }
    
    protected double calculateThroughputPerSecond(long totalDurationInMs, int nrOfIterations) {
        double totalDurationInSeconds = ((double) totalDurationInMs) / 1000.0;
        return ((double) nrOfIterations) / totalDurationInSeconds;
    }
    
    public void writeOutput() {
        try(PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFileName), Charset.forName("UTF-8")))) {
            
            StringBuilder strb = new StringBuilder();
            strb.append("process,metric,");
            for (int i = 1; i <= maxNrOfThreads; i++) {
                strb.append(i);
                strb.append(",");
            }
            writer.println(strb.toString());
            
            for (ProcessOutput processOutput : processOutputs) {
                writeAverages(writer, processOutput);
                writeStddevs(writer, processOutput);
                writeThrougput(writer, processOutput);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    protected StringBuilder writeAverages(PrintWriter writer, ProcessOutput processOutput) {
        StringBuilder strb = new StringBuilder();
        strb.append(processOutput.getName());
        strb.append(",");
        strb.append("average,");
        for (double avg : processOutput.getAverages()) {
            strb.append(Precision.round(avg, 2));
            strb.append(",");    
        }
        writer.println(strb.toString());
        return strb;
    }
    
    protected StringBuilder writeStddevs(PrintWriter writer, ProcessOutput processOutput) {
        StringBuilder strb = new StringBuilder();
        strb.append(processOutput.getName());
        strb.append(",");
        strb.append("stddev,");
        for (double stddev : processOutput.getStddevs()) {
            strb.append(Precision.round(stddev, 2));
            strb.append(",");    
        }
        writer.println(strb.toString());
        return strb;
    }
    
    protected StringBuilder writeThrougput(PrintWriter writer, ProcessOutput processOutput) {
        StringBuilder strb = new StringBuilder();
        strb.append(processOutput.getName());
        strb.append(",");
        strb.append("throughputPerSecond,");
        for (double througput : processOutput.getThroughputPerSeconds()) {
            strb.append(Precision.round(througput, 2));
            strb.append(",");    
        }
        writer.println(strb.toString());
        return strb;
    }
    
    public static class ProcessOutput {
        
        protected String process;
        
        // index is the number of threads for these arrays
        
        protected double[] averages;
        protected double[] stddevs;
        protected double[] throughputPerSeconds;
        
        public ProcessOutput(String name, int maxNrOfThreads) {
            this.process = name;
            this.averages = new double[maxNrOfThreads];
            this.stddevs = new double[maxNrOfThreads];
            this.throughputPerSeconds = new double[maxNrOfThreads];
        }
        
        public void addMeasurement(int nrOfThreads, double average, double stddev, double throughputPerSecond) {
            averages[nrOfThreads - 1] = average;
            stddevs[nrOfThreads - 1] = stddev;
            throughputPerSeconds[nrOfThreads - 1] = throughputPerSecond;
        }

        public String getName() {
            return process;
        }

        public void setName(String name) {
            this.process = name;
        }

        public double[] getAverages() {
            return averages;
        }

        public void setAverages(double[] averages) {
            this.averages = averages;
        }

        public double[] getStddevs() {
            return stddevs;
        }

        public void setStddevs(double[] stddevs) {
            this.stddevs = stddevs;
        }

        public double[] getThroughputPerSeconds() {
            return throughputPerSeconds;
        }

        public void setThroughputPerSeconds(double[] throughputPerSeconds) {
            this.throughputPerSeconds = throughputPerSeconds;
        }
        
    }

}
