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

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.runnable.AllSequentialServiceTasks;
import org.flowable.runnable.BenchmarkRunnable;
import org.flowable.runnable.ManyVariablesRunnable;
import org.flowable.runnable.StartToEndRunnable;
import org.flowable.runnable.SubprocessesRunnable;
import org.flowable.runnable.TerminateUserTasksRunnable;

import com.zaxxer.hikari.HikariDataSource;

public class Benchmark {
    
    public static Properties properties;
    
    public static ProcessEngine processEngine;
    public static RepositoryService repositoryService;
    public static RuntimeService runtimeService;
    public static TaskService taskService;
    public static ManagementService managementService;
    public static HistoryService historyService;
    
    public static void main(String[] args) throws Exception {
        
        properties = new Properties();
        properties.load(new InputStreamReader(new FileInputStream("config.properties"), "UTF-8"));
        
        int minNrOfThreads = Integer.valueOf(properties.getProperty("min-threads"));
        int maxNrOfThreads = Integer.valueOf(properties.getProperty("max-threads"));
        int nrOfIterations = Integer.valueOf(properties.getProperty("iterations"));
        String outputName = properties.getProperty("outputname");
        
        List<String> processes = Arrays.asList("startToEnd", "allSequentialServiceTasks", "parallelSubprocesses", "manyVariables", "terminateUserTasks");

        System.out.println("Running iterations for warming up JVM");
        executeBenchmark(true, minNrOfThreads, maxNrOfThreads, 1000, null, processes); // warming up with 1000 iterations
        System.out.println("JVM warmup done");
        executeBenchmark(false, minNrOfThreads, maxNrOfThreads, nrOfIterations, outputName, processes);
        System.out.println("All done.");
    }

    protected static void executeBenchmark(boolean warmup, int minNrOfThreads, int maxNrOfThreads, int nrOfIterations, String outputName, List<String> processes) throws Exception {
        Output output = new Output(outputName, maxNrOfThreads, nrOfIterations);
        
        for (int nrOfThreads = minNrOfThreads; nrOfThreads <= maxNrOfThreads; nrOfThreads++) {
            
            for (int processIndex = 0; processIndex < processes.size(); processIndex++) {
                
                String process = processes.get(processIndex);
                
                System.out.println();
                System.out.println("Setting up the process engine for process " + process);
                createProcessEngine();
                deployProcesses();
                
                System.out.println("Creating executor service for process " + process + " with " + nrOfThreads + " threads");
                ThreadFactory threadFactory = Executors.defaultThreadFactory();
                ExecutorService executorService = new ThreadPoolExecutor(nrOfThreads, nrOfThreads, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
                
                System.out.println("Creating runnables for process " + process);
                ArrayList<BenchmarkRunnable> runnables = new ArrayList<BenchmarkRunnable>(nrOfIterations);
                for (int runnableIndex = 0; runnableIndex < nrOfIterations; runnableIndex++) {
                    runnables.add(createRunnable(process));
                }
                
                System.out.println("Submitting runnables for process " + process);
                long startTime = System.currentTimeMillis();
                for (int runnableIndex = 0; runnableIndex < runnables.size(); runnableIndex++) {
                    executorService.submit(runnables.get(runnableIndex));
                }
                
                executorService.shutdown();
                executorService.awaitTermination(60, TimeUnit.MINUTES);
                long endTime = System.currentTimeMillis();
                
                long totalTime = endTime - startTime;
                
                System.out.println("Calculating metrics ...");
                output.addResults(process, nrOfThreads, nrOfIterations, totalTime, runnables);
                System.out.println(nrOfIterations + " iterations for process " + process + " with " + nrOfThreads + " threads completed");
                System.out.println("Took " + totalTime + " ms");
                
            }
            
        }
        
        if (!warmup) {
            System.out.println("Done. Writing output.");
            output.writeOutput();
        }
    }

    protected static ProcessEngine createProcessEngine() {
        
        if (processEngine != null) {
            System.out.println("Closing current process engine");
            processEngine.close();
            ((HikariDataSource) processEngine.getProcessEngineConfiguration().getDataSource()).close(); // just to be extra sure
            processEngine = null;
        }
        
        System.out.println("Creating new process engine");
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(createDatabsource());
        processEngineConfiguration.setDatabaseSchemaUpdate("drop-create");
        processEngineConfiguration.setEnableEventDispatcher(false);
        
        String historyLevel = (String) properties.get("history-level");
        System.out.println("History level = " + historyLevel);
        if (historyLevel.equalsIgnoreCase("none")) {
            processEngineConfiguration.setHistoryLevel(HistoryLevel.NONE);
        } else {
            processEngineConfiguration.setHistoryLevel(HistoryLevel.AUDIT);
        }
        
        boolean enableAsyncHistory = Boolean.valueOf(properties.getProperty("async-history"));
        boolean enableGrouping = Boolean.valueOf(properties.getProperty("async-history-grouping"));
        boolean enableGzip = Boolean.valueOf(properties.getProperty("async-history-gzip"));
        int groupThreshold = Integer.valueOf(properties.getProperty("async-history-grouping-threshold", "10"));
        if (enableAsyncHistory) {
            System.out.println("Running with async history enabled, grouping = " + enableGrouping + "," + "gzip = " + enableGzip + ", threshold = " + groupThreshold);
            processEngineConfiguration.setAsyncHistoryEnabled(true);
            processEngineConfiguration.setAsyncExecutorActivate(true);
            processEngineConfiguration.setAsyncHistoryJsonGroupingEnabled(enableGrouping);
            processEngineConfiguration.setAsyncHistoryJsonGzipCompressionEnabled(enableGzip);
            processEngineConfiguration.setAsyncHistoryJsonGroupingThreshold(groupThreshold);
        }
        
        boolean enableTreeFetch = Boolean.valueOf(properties.getProperty("tree-fetch"));
        if (!enableTreeFetch) {
            processEngineConfiguration.getPerformanceSettings().setEnableEagerExecutionTreeFetching(false);
        }
        
        processEngine = processEngineConfiguration.buildProcessEngine();
        
        repositoryService = processEngine.getRepositoryService();
        runtimeService = processEngine.getRuntimeService();
        managementService = processEngine.getManagementService();
        taskService = processEngine.getTaskService();
        historyService = processEngine.getHistoryService();
       
        return processEngine;
    }

    protected static HikariDataSource createDatabsource() {
        String jdbcUrl = properties.getProperty("jdbc-url");
        String jdbcDriver = properties.getProperty("jdbc-driver");
        String jdbcUsername = properties.getProperty("jdbc-user");
        String jdbcPassword = properties.getProperty("jdbc-password");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setDriverClassName(jdbcDriver);
        dataSource.setUsername(jdbcUsername);
        dataSource.setPassword(jdbcPassword);
        dataSource.setMaximumPoolSize(50);
        return dataSource;
    }
    
    protected static void deployProcesses() {
        repositoryService.createDeployment()
            .addClasspathResource("allSequentialServiceTasks.bpmn20.xml")
            .addClasspathResource("manyVariables.bpmn20.xml")
            .addClasspathResource("parallelSubprocesses.bpmn20.xml")
            .addClasspathResource("startToEnd.bpmn20.xml")
            .addClasspathResource("terminateUserTasks.bpmn20.xml")
            .deploy();
        System.out.println("Deployment done");
    }
    
    protected static BenchmarkRunnable createRunnable(String process) {
        if (process.equals("startToEnd")) {
            return new StartToEndRunnable();
        } else if (process.equals("allSequentialServiceTasks")) {
            return new AllSequentialServiceTasks();
        } else if (process.equals("parallelSubprocesses")) {
            return new SubprocessesRunnable();
        } else if (process.equals("manyVariables")) {
            return new ManyVariablesRunnable();
        } else if (process.equals("terminateUserTasks")) {
            return new TerminateUserTasksRunnable();
        } else {
            throw new RuntimeException("Invalid process : " + process);
        }
    }
    

}
