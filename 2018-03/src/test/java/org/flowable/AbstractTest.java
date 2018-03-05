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

import org.flowable.engine.repository.Deployment;
import org.flowable.engine.test.FlowableRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractTest {
    
    @Rule
    public FlowableRule flowableRule = new FlowableRule();
    
    @Before
    public void initServices() {
        Benchmark.processEngine = flowableRule.getProcessEngine();
        Benchmark.runtimeService = flowableRule.getRuntimeService();
        Benchmark.repositoryService = flowableRule.getRepositoryService();
        Benchmark.managementService = flowableRule.getManagementService();
        Benchmark.historyService = flowableRule.getHistoryService();
        Benchmark.taskService = flowableRule.getTaskService();
    }
    
    @After
    public void deleteAllData() {
        for (Deployment deployment : flowableRule.getRepositoryService().createDeploymentQuery().list()) {
            flowableRule.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }
    
}
