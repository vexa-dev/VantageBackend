package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;

/**
 * Mapper manual entre {@link WorkflowDefinitionJpaEntity} (infraestructura) y
 * {@link WorkflowDefinition} (dominio puro). Delega la parte JSONB a
 * {@link WorkflowDefinitionJsonCodec}.
 */
public final class WorkflowDefinitionMapper {

    private WorkflowDefinitionMapper() {
    }

    public static WorkflowDefinition toDomain(WorkflowDefinitionJpaEntity entity) {
        return WorkflowDefinitionJsonCodec.decode(
                WorkflowDefinitionId.of(String.valueOf(entity.getId())),
                entity.getName(),
                entity.getStatesAndTransitions());
    }

    /**
     * Convierte {@code domain} a una entidad JPA nueva o existente.
     * {@code tenant_id} se toma de {@code tenantId} (resuelto por el
     * adapter vía {@code TenantContext}, nunca por el propio dominio —
     * {@link WorkflowDefinition} no modela tenant, es "tenant-ambient" como
     * documenta {@code WorkflowDefinitionRepositoryPort}). {@code
     * is_builtin_template} siempre se guarda {@code false}: ningún método de
     * este puerto vuelve a marcar una fila como plantilla built-in — esa
     * columna solo la establece {@code V2__builtin_workflow_templates.sql}.
     */
    public static WorkflowDefinitionJpaEntity toEntity(WorkflowDefinition domain, Long tenantId) {
        WorkflowDefinitionJpaEntity entity = new WorkflowDefinitionJpaEntity();
        if (domain.id() != null) {
            entity.setId(Long.parseLong(domain.id().value()));
        }
        entity.setTenantId(tenantId);
        entity.setName(domain.name());
        entity.setBuiltInTemplate(false);
        entity.setStatesAndTransitions(WorkflowDefinitionJsonCodec.encode(domain));
        return entity;
    }
}
