package org.flowable.benchmark.app;

import org.flowable.benchmark.app.runnable.BenchmarkRunnable;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.spring.configurator.DefaultAutoDeploymentStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Filip Hrisafov
 */
@Service
public class BenchmarkService implements CommandLineRunner, ApplicationContextAware {

    protected final Logger logger = LoggerFactory.getLogger(getClass());


    protected final BenchmarkProperties properties;
    protected final ProcessEngine processEngine;
    protected final Map<String, BenchmarkRunnable> benchmarkRunnableMap;

    protected ApplicationContext applicationContext;

    public BenchmarkService(BenchmarkProperties properties, ProcessEngine processEngine, ObjectProvider<BenchmarkRunnable> benchmarkRunnables) {
        this.properties = properties;
        this.processEngine = processEngine;
        this.benchmarkRunnableMap = benchmarkRunnables.orderedStream()
            .collect(Collectors.toMap(BenchmarkRunnable::getDescription, Function.identity()));
    }

    @Override
    public void run(String... args) throws Exception {
        String outputName = properties.getOutputName();
        int minNrOfThreads = properties.getMinThreads();
        int maxNrOfThreads = properties.getMaxThreads();
        int nrOfIterations = properties.getIterations();

        Collection<String> processes = properties.getProcesses();
        if (processes.isEmpty()) {
            processes = benchmarkRunnableMap.keySet();
        }
        logger.info("Running 1000 iterations for warming up JVM");
        executeBenchmark(minNrOfThreads, maxNrOfThreads, 1000, processes);
        logger.info("JVM warmup done");
        logger.info("Running Benchmark with {} iterations for processes {}", nrOfIterations, processes);
        Output output = executeBenchmark(minNrOfThreads, maxNrOfThreads, nrOfIterations, processes);

        logger.info("Done. Writing output");
        output.writeOutput(outputName);
    }

    protected Output executeBenchmark(int minNrOfThreads, int maxNrOfThreads, int nrOfIterations, Collection<String> processes) throws Exception {
        Output output = new Output(maxNrOfThreads, nrOfIterations);
        long benchmarkStart = System.currentTimeMillis();
        for (int nrOfThreads = minNrOfThreads; nrOfThreads <= maxNrOfThreads; nrOfThreads++) {

            for (String process : processes) {

                BenchmarkRunnable runnable = benchmarkRunnableMap.get(process);

                if (runnable == null) {
                    logger.error("There is no benchmark for process {}. Skipping.", process);
                    continue;
                }

                logger.info("Setup process engine for process {}", process);
                dropCreateDb();
                deployProcesses();

                logger.info("Creating executor service for process {} with {} threads", process, nrOfThreads);
                ThreadFactory threadFactory = Executors.defaultThreadFactory();
                ThreadPoolExecutor executorService = new ThreadPoolExecutor(nrOfThreads, nrOfThreads, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), threadFactory);

                List<Future<Long>> timingFutures = new ArrayList<>(nrOfIterations);

                logger.info("Submitting runnables for process {}", process);
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < nrOfIterations; i++) {
                    timingFutures.add(executorService.submit(runnable));
                }

                executorService.shutdown();
                executorService.awaitTermination(60, TimeUnit.MINUTES);
                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;

                List<Long> timings = new ArrayList<>(timingFutures.size());
                for (Future<Long> timingFuture : timingFutures) {
                    timings.add(timingFuture.get());
                }

                logger.info("Calculating metrics ...");
                output.addResults(process, nrOfThreads, nrOfIterations, totalTime, timings);

                logger.info("{} iterations for process {} with {} threads completed", nrOfIterations, process, nrOfThreads);
                logger.info("Took {} ms for process {} and {} threads", totalTime, process, nrOfThreads);
            }
        }

        long benchmarkEnd = System.currentTimeMillis();
        long benchmarkTotalTime = benchmarkEnd - benchmarkStart;

        logger.info("Benchmark took {} ms for {} iterations", benchmarkTotalTime, nrOfIterations);

        return output;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    protected void dropCreateDb() {
        processEngine.getProcessEngineConfiguration().getAsyncExecutor().shutdown();
        processEngine.getManagementService()
            .executeCommand(commandContext -> {
                org.flowable.eventregistry.impl.util.CommandContextUtil.getEventRegistryConfiguration(commandContext)
                    .getSchemaManager()
                    .schemaDrop();
                return null;
            });

        processEngine.getManagementService()
            .executeCommand(commandContext -> {
                SchemaManager schemaManager = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                    .getSchemaManager();
                schemaManager.schemaCreate();

                org.flowable.idm.engine.impl.util.CommandContextUtil.getIdmEngineConfiguration(commandContext)
                    .getSchemaManager()
                    .schemaCreate();

                org.flowable.eventregistry.impl.util.CommandContextUtil.getEventRegistryConfiguration(commandContext)
                    .getSchemaManager()
                    .schemaCreate();
                return null;
            });
        processEngine.getProcessEngineConfiguration().getAsyncExecutor().start();
    }

    protected void deployProcesses() {
        AutoDeploymentStrategy<ProcessEngine> deploymentStrategy = new DefaultAutoDeploymentStrategy();

        try {
            Resource[] resources = applicationContext.getResources("classpath*:benchmark-processes/*");
            deploymentStrategy.deployResources("benchmark", resources, processEngine);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
