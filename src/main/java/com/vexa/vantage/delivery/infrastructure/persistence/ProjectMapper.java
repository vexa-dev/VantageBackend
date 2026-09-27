package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.TenantId;

/**
 * Mapper manual entre {@link ProjectJpaEntity} (infraestructura) y
 * {@link Project} (dominio puro).
 */
public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static Project toDomain(ProjectJpaEntity entity) {
        return new Project(
                ProjectId.of(String.valueOf(entity.getId())),
                TenantId.of(String.valueOf(entity.getTenantId())),
                entity.getName(),
                entity.getDescription(),
                WorkflowDefinitionId.of(String.valueOf(entity.getWorkflowDefinitionId())),
                entity.getStatus());
    }

    /**
     * Convierte {@code domain} a una entidad JPA nueva o existente, según
     * tenga o no {@code id} (creación vs. actualización). El {@code tenant_id}
     * se copia directamente de {@link Project#tenantId()} — a diferencia de
     * los agregados "tenant-ambient" de este mismo contexto ({@code WorkItem},
     * {@code WorkflowDefinition}), {@link Project} SÍ modela su propio
     * {@code TenantId} (igual que {@code AppUser} en identity), así que no
     * hace falta que el adapter lo derive de {@code TenantContext}.
     */
    public static ProjectJpaEntity toEntity(Project domain) {
        ProjectJpaEntity entity = new ProjectJpaEntity();
        if (domain.id() != null) {
            entity.setId(Long.parseLong(domain.id().value()));
        }
        entity.setTenantId(Long.parseLong(domain.tenantId().value()));
        entity.setName(domain.name());
        entity.setDescription(domain.description());
        entity.setWorkflowDefinitionId(Long.parseLong(domain.workflowDefinitionId().value()));
        entity.setStatus(domain.status());
        return entity;
    }
}
