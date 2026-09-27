package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.application.WorkflowDefinitionRepositoryPort;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;
import com.vexa.vantage.shared.infrastructure.TenantContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter (arquitectura hexagonal) que implementa
 * {@link WorkflowDefinitionRepositoryPort} sobre JPA/Spring Data.
 *
 * <p>{@link #save} siempre estampa {@code tenant_id} desde
 * {@link TenantContext#require()}, nunca desde el dominio (que no lo
 * modela) — esto es justo lo que hace que aprovisionar la copia de una
 * plantilla built-in (propiedad del tenant de sistema) para un proyecto
 * nuevo termine perteneciendo al tenant del actor, no al tenant de sistema.
 */
@Component
public class WorkflowDefinitionRepositoryAdapter implements WorkflowDefinitionRepositoryPort {

    private final WorkflowDefinitionJpaRepository repository;

    public WorkflowDefinitionRepositoryAdapter(WorkflowDefinitionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<WorkflowDefinition> findById(WorkflowDefinitionId id) {
        return repository.findById(Long.parseLong(id.value())).map(WorkflowDefinitionMapper::toDomain);
    }

    @Override
    public Optional<WorkflowDefinition> findBuiltInTemplate() {
        return repository.findByBuiltInTemplateTrue().map(WorkflowDefinitionMapper::toDomain);
    }

    @Override
    public WorkflowDefinition save(WorkflowDefinition definition) {
        Long tenantId = Long.parseLong(TenantContext.require().value());
        WorkflowDefinitionJpaEntity saved = repository.save(WorkflowDefinitionMapper.toEntity(definition, tenantId));
        return WorkflowDefinitionMapper.toDomain(saved);
    }
}
