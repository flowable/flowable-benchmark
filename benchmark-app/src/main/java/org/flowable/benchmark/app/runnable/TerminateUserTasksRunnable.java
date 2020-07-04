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

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("terminateUserTasks")
public class TerminateUserTasksRunnable extends BenchmarkRunnable {

    protected static Map<String, Object> variables = new HashMap<>();

    static {
        for (int i = 0; i < 10; i++) {
            variables.put("variable_" + i, "value_" + i);
        }
    }

    protected final RuntimeService runtimeService;
    protected final TaskService taskService;

    public TerminateUserTasksRunnable(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @Override
    protected void executeRun() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("terminateUserTasks", variables);
        Task task = taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .taskName("A")
            .singleResult();

        // Triggering A throws the process-instance scoped signal event
        taskService.complete(task.getId());
    }

    @Override
    public String getDescription() {
        return "terminateUserTasks";
    }

}
