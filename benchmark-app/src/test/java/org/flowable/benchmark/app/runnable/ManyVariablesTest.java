package org.flowable.benchmark.app.runnable;

import org.flowable.benchmark.app.BaseTest;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Filip Hrisafov
 */
class ManyVariablesTest extends BaseTest {

    @Autowired
    protected RepositoryService repositoryService;

    @Test
    @Deployment(resources = "benchmark-processes/manyVariables.bpmn20.xml")
    void testProcess() {
        assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();

        ManyVariablesRunnable runnable = new ManyVariablesRunnable(runtimeService, taskService);
        Long totalTime = runnable.call();
        assertThat(totalTime).isGreaterThan(0);
        assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().finished().count()).isEqualTo(1);

        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
            .finished()
            .singleResult();
        assertThat(historicTaskInstance.getName()).isEqualTo("A");


        assertThat(historyService.createHistoricVariableInstanceQuery()
            .count()).isEqualTo(52); // 50 generated, 2 for the gw vars
    }

}
