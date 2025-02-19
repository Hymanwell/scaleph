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

package cn.sliew.scaleph.workflow.service.impl;

import cn.sliew.scaleph.common.dict.workflow.WorkflowInstanceState;
import cn.sliew.scaleph.dao.entity.master.workflow.WorkflowInstance;
import cn.sliew.scaleph.dao.entity.master.workflow.WorkflowInstanceVO;
import cn.sliew.scaleph.dao.mapper.master.workflow.WorkflowInstanceMapper;
import cn.sliew.scaleph.workflow.service.WorkflowInstanceService;
import cn.sliew.scaleph.workflow.service.convert.WorkflowInstanceVOConvert;
import cn.sliew.scaleph.workflow.service.dto.WorkflowInstanceDTO;
import cn.sliew.scaleph.workflow.service.param.WorkflowInstanceListParam;
import cn.sliew.scaleph.workflow.statemachine.WorkflowInstanceStateMachine;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static cn.sliew.milky.common.check.Ensures.checkState;

@Service
public class WorkflowInstanceServiceImpl implements WorkflowInstanceService {

    @Autowired
    private WorkflowInstanceMapper workflowInstanceMapper;
    @Autowired
    private WorkflowInstanceStateMachine stateMachine;

    @Override
    public Page<WorkflowInstanceDTO> list(WorkflowInstanceListParam param) {
        Page<WorkflowInstance> page = new Page<>(param.getCurrent(), param.getPageSize());
        Page<WorkflowInstanceVO> workflowInstancePage = workflowInstanceMapper.list(page, param.getWorkflowDefinitionId(), param.getState());
        Page<WorkflowInstanceDTO> result = new Page<>(workflowInstancePage.getCurrent(), workflowInstancePage.getSize(), workflowInstancePage.getTotal());
        List<WorkflowInstanceDTO> workflowDefinitionDTOS = WorkflowInstanceVOConvert.INSTANCE.toDto(workflowInstancePage.getRecords());
        result.setRecords(workflowDefinitionDTOS);
        return result;
    }

    @Override
    public WorkflowInstanceDTO get(Long id) {
        WorkflowInstanceVO vo = workflowInstanceMapper.get(id);
        checkState(vo != null, () -> "workflow instance not exists for id: " + id);
        return WorkflowInstanceVOConvert.INSTANCE.toDto(vo);
    }

    @Override
    public void updateState(Long id, WorkflowInstanceState state, WorkflowInstanceState nextState, String message) {
        LambdaUpdateWrapper<WorkflowInstance> updateWrapper = Wrappers.lambdaUpdate(WorkflowInstance.class)
                .eq(WorkflowInstance::getId, id)
                .eq(WorkflowInstance::getState, state);
        WorkflowInstance record = new WorkflowInstance();
        record.setState(nextState);
        record.setMessage(message);
        workflowInstanceMapper.update(record, updateWrapper);
    }

    @Override
    public void updateSuccess(Long id) {
        WorkflowInstance record = new WorkflowInstance();
        record.setId(id);
        record.setState(WorkflowInstanceState.SUCCESS);
        record.setEndTime(new Date());
        workflowInstanceMapper.updateById(record);
    }

    @Override
    public void updateFailure(Long id, Throwable throwable) {
        WorkflowInstance record = new WorkflowInstance();
        record.setId(id);
        record.setState(WorkflowInstanceState.FAILURE);
        record.setEndTime(new Date());
        if (throwable != null) {
            record.setMessage(throwable.getMessage());
        }
        workflowInstanceMapper.updateById(record);
    }

    @Override
    public void updateTaskId(Long id, String taskId) {
        WorkflowInstance record = new WorkflowInstance();
        record.setId(id);
        record.setTaskId(taskId);
        workflowInstanceMapper.updateById(record);
    }

    @Override
    public WorkflowInstanceDTO deploy(Long workflowDefinitionId) {
        WorkflowInstance record = new WorkflowInstance();
        record.setWorkflowDefinitionId(workflowDefinitionId);
        record.setState(WorkflowInstanceState.PENDING);
        workflowInstanceMapper.insert(record);
        stateMachine.deploy(get(record.getId()));
        return get(record.getId());
    }

    @Override
    public void shutdown(Long id) {
        stateMachine.shutdown(get(id));
    }

    @Override
    public void suspend(Long id) {
        stateMachine.suspend(get(id));
    }

    @Override
    public void resume(Long id) {
        stateMachine.resume(get(id));
    }
}
