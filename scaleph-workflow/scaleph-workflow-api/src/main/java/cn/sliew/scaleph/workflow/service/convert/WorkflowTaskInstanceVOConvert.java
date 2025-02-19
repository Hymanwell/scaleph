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

package cn.sliew.scaleph.workflow.service.convert;

import cn.sliew.scaleph.common.convert.BaseConvert;
import cn.sliew.scaleph.dao.entity.master.workflow.WorkflowTaskInstanceVO;
import cn.sliew.scaleph.workflow.service.dto.WorkflowTaskInstanceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.BeanUtils;

@Mapper(uses = {}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkflowTaskInstanceVOConvert extends BaseConvert<WorkflowTaskInstanceVO, WorkflowTaskInstanceDTO> {
    WorkflowTaskInstanceVOConvert INSTANCE = Mappers.getMapper(WorkflowTaskInstanceVOConvert.class);

    @Override
    default WorkflowTaskInstanceVO toDo(WorkflowTaskInstanceDTO dto) {
        WorkflowTaskInstanceVO entity = new WorkflowTaskInstanceVO();
        BeanUtils.copyProperties(dto, entity);
        entity.setWorkflowTaskDefinition(WorkflowTaskDefinitionConvert.INSTANCE.toDo(dto.getWorkflowTaskDefinition()));
        entity.setWorkflowInstance(WorkflowInstanceVOConvert.INSTANCE.toDo(dto.getWorkflowInstanceDTO()));
        return entity;
    }

    @Override
    default WorkflowTaskInstanceDTO toDto(WorkflowTaskInstanceVO entity) {
        WorkflowTaskInstanceDTO dto = new WorkflowTaskInstanceDTO();
        BeanUtils.copyProperties(entity, dto);
        dto.setWorkflowTaskDefinition(WorkflowTaskDefinitionConvert.INSTANCE.toDto(entity.getWorkflowTaskDefinition()));
        dto.setWorkflowInstanceDTO(WorkflowInstanceVOConvert.INSTANCE.toDto(entity.getWorkflowInstance()));
        return dto;
    }
}
