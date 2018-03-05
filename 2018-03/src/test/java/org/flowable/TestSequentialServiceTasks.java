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

import org.flowable.runnable.AllSequentialServiceTasks;
import org.junit.Test;

public class TestSequentialServiceTasks extends AbstractTest {
    
    @Test
    public void testProcess() {
        flowableRule.getRepositoryService().createDeployment().addClasspathResource("allSequentialServiceTasks.bpmn20.xml").deploy();
        assertEquals(0, flowableRule.getHistoryService().createHistoricProcessInstanceQuery().count());
        
        AllSequentialServiceTasks runnable = new AllSequentialServiceTasks();
        runnable.run();
        
        assertTrue(runnable.getStartTime() > 0);
        assertTrue(runnable.getEndTime() > 0);
        assertEquals(1, flowableRule.getHistoryService().createHistoricProcessInstanceQuery().finished().count());
        assertEquals(12, flowableRule.getHistoryService().createHistoricActivityInstanceQuery().count()); // 10 service tasks + start + end
    }

}
