package org.flowable.benchmark.app;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(FlowableSpringExtension.class)
public abstract class BaseTest {

    @Autowired
    protected ProcessEngine processEngine;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected ManagementService managementService;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected TaskService taskService;
}
