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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.flowable.runnable.ManyVariablesRunnable;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Test;

public class TestManyVariables extends AbstractTest {
    
    @Test
    public void testProcess() {
        flowableRule.getRepositoryService().createDeployment().addClasspathResource("manyVariables.bpmn20.xml").deploy();
        assertEquals(0, flowableRule.getHistoryService().createHistoricProcessInstanceQuery().count());
        
        ManyVariablesRunnable runnable = new ManyVariablesRunnable();
        runnable.run();
        
        assertTrue(runnable.getStartTime() > 0);
        assertTrue(runnable.getEndTime() > 0);
        assertEquals(1, flowableRule.getHistoryService().createHistoricProcessInstanceQuery().finished().count());
        assertEquals(1, flowableRule.getHistoryService().createHistoricTaskInstanceQuery().finished().count());
        
        HistoricTaskInstance historicTaskInstance = flowableRule.getHistoryService().createHistoricTaskInstanceQuery().finished().singleResult();
        assertEquals("A", historicTaskInstance.getName());
        
        assertEquals(52, flowableRule.getHistoryService().createHistoricVariableInstanceQuery().count()); // 50 generated, 2 for the gw vars
    }

}
