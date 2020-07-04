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

package org.flowable.benchmark.app.runnable;

import org.flowable.benchmark.app.BaseTest;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SequentialServiceTasksTest extends BaseTest {

    @Test
    @Deployment(resources = "benchmark-processes/allSequentialServiceTasks.bpmn20.xml")
    void testProcess() {
        assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();

        AllSequentialServiceTasks runnable = new AllSequentialServiceTasks(runtimeService);
        Long totalTime = runnable.call();
        assertThat(totalTime).isGreaterThan(0);
        assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isEqualTo(1);
        assertThat(historyService.createHistoricActivityInstanceQuery()
            .count()).isEqualTo(12); // 10 service tasks + start + end
    }
}
