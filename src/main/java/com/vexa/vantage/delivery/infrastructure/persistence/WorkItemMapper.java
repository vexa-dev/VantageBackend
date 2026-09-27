package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;
import com.vexa.vantage.shared.domain.WorkItemId;

/**
 * Mapper manual entre {@link WorkItemJpaEntity} (infraestructura) y
 * {@link WorkItem} (dominio puro).
 *
 * <p>{@code parentWorkItemId}/{@code sprintWorkItemId}/{@code assigneeId} se
 * aplican DESPUÉS del constructor (vía {@code changeParent}/
 * {@code assignToSprint}/{@code assignTo}), ya que el constructor de
 * {@link WorkItem} solo recibe los campos obligatorios.
 */
public final class WorkItemMapper {

    private WorkItemMapper() {
    }

    public static WorkItem toDomain(WorkItemJpaEntity entity) {
        WorkItem workItem = new WorkItem(
                WorkItemId.of(String.valueOf(entity.getId())),
                ProjectId.of(String.valueOf(entity.getProjectId())),
                entity.getType(),
                entity.getState(),
                entity.getTitle(),
                entity.getBusinessValue(),
                entity.getUrgency(),
                entity.getStoryPoints());
        workItem.changeDescription(entity.getDescription());
        if (entity.getParentWorkItemId() != null) {
            workItem.changeParent(WorkItemId.of(String.valueOf(entity.getParentWorkItemId())));
        }
        if (entity.getSprintWorkItemId() != null) {
            workItem.assignToSprint(WorkItemId.of(String.valueOf(entity.getSprintWorkItemId())));
        }
        if (entity.getAssigneeId() != null) {
            workItem.assignTo(UserId.of(String.valueOf(entity.getAssigneeId())));
        }
        workItem.changeTshirtSize(entity.getTshirtSize());
        return workItem;
    }

    /**
     * Convierte {@code domain} a una entidad JPA nueva o existente.
     * {@code tenant_id} se toma de {@code tenantId} (resuelto por el adapter
     * vía {@code TenantContext}, nunca del dominio — {@link WorkItem} no
     * modela tenant, es "tenant-ambient" como documenta
     * {@code WorkItemRepositoryPort}).
     */
    public static WorkItemJpaEntity toEntity(WorkItem domain, Long tenantId) {
        WorkItemJpaEntity entity = new WorkItemJpaEntity();
        if (domain.id() != null) {
            entity.setId(Long.parseLong(domain.id().value()));
        }
        entity.setTenantId(tenantId);
        entity.setProjectId(Long.parseLong(domain.projectId().value()));
        entity.setParentWorkItemId(domain.parentWorkItemId().map(id -> Long.parseLong(id.value())).orElse(null));
        entity.setSprintWorkItemId(domain.sprintWorkItemId().map(id -> Long.parseLong(id.value())).orElse(null));
        entity.setType(domain.type());
        entity.setState(domain.state());
        entity.setTitle(domain.title());
        entity.setDescription(domain.description());
        entity.setBusinessValue(domain.businessValue());
        entity.setUrgency(domain.urgency());
        entity.setStoryPoints(domain.storyPoints());
        entity.setAssigneeId(domain.assigneeId().map(id -> Long.parseLong(id.value())).orElse(null));
        entity.setTshirtSize(domain.tshirtSize().orElse(null));
        return entity;
    }
}
