/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.sliew.scaleph.workflow.listener.workflowinstance;

import cn.sliew.milky.common.util.JacksonUtil;
import cn.sliew.scaleph.workflow.service.WorkflowInstanceService;
import cn.sliew.scaleph.workflow.service.WorkflowTaskDefinitionService;
import cn.sliew.scaleph.workflow.service.WorkflowTaskInstanceService;
import cn.sliew.scaleph.workflow.service.dto.WorkflowDefinitionDTO;
import cn.sliew.scaleph.workflow.service.dto.WorkflowInstanceDTO;
import cn.sliew.scaleph.workflow.service.dto.WorkflowTaskDefinitionDTO;
import com.google.common.graph.Graph;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.annotation.RInject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class WorkflowInstanceDeployEventListener extends AbstractWorkflowInstanceEventListener {

    @Override
    protected CompletableFuture handleEventAsync(WorkflowInstanceEventDTO event) {
        CompletableFuture<?> future = executorService.submit(new DeployRunner(event)).toCompletableFuture();
        future.whenCompleteAsync((unused, throwable) -> {
            if (throwable != null) {
                onFailure(event.getWorkflowInstanceId(), throwable);
            }
        });
        return future;
    }

    /**
     * 必须实现 Serializable 接口，无法使用 lambda
     */
    public static class DeployRunner implements Runnable, Serializable {

        private WorkflowInstanceEventDTO event;

        @RInject
        private String taskId;
        @Autowired
        private WorkflowInstanceService workflowInstanceService;
        @Autowired
        private WorkflowTaskDefinitionService workflowTaskDefinitionService;
        @Autowired
        private WorkflowTaskInstanceService workflowTaskInstanceService;

        public DeployRunner(WorkflowInstanceEventDTO event) {
            this.event = event;
        }

        @Override
        public void run() {
            workflowInstanceService.updateTaskId(event.getWorkflowInstanceId(), taskId);
            workflowInstanceService.updateState(event.getWorkflowInstanceId(), event.getState(), event.getNextState(), null);
            WorkflowInstanceDTO workflowInstanceDTO = workflowInstanceService.get(event.getWorkflowInstanceId());
            WorkflowDefinitionDTO workflowDefinitionDTO = workflowInstanceDTO.getWorkflowDefinition();

            // 找到 root 节点，批量启动 root 节点
            Graph<WorkflowTaskDefinitionDTO> dag = workflowTaskDefinitionService.getDag(workflowDefinitionDTO.getId());
            Set<WorkflowTaskDefinitionDTO> nodes = dag.nodes();
            for (WorkflowTaskDefinitionDTO workflowTaskDefinitionDTO : nodes) {
                System.out.println("验证是否需要启动 root 节点: " + JacksonUtil.toJsonString(workflowTaskDefinitionDTO));
                // root 节点
                if (dag.inDegree(workflowTaskDefinitionDTO) == 0) {
                    workflowTaskInstanceService.deploy(workflowTaskDefinitionDTO.getId(), event.getWorkflowInstanceId());
                    System.out.println("真正启动 root 节点: " + JacksonUtil.toJsonString(workflowTaskDefinitionDTO));
                }
            }
            // todo 循环检测 workflowTaskInstanceDTOS 状态或接收 workflowTaskInstance 事件，判断是否成功或失败
        }
    }
}
