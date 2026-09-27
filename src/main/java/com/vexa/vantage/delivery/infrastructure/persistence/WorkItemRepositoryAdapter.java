package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.application.WorkItemRepositoryPort;
import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.WorkItemId;
import com.vexa.vantage.shared.infrastructure.TenantContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter (arquitectura hexagonal) que implementa
 * {@link WorkItemRepositoryPort} sobre JPA/Spring Data.
 *
 * <p>{@link #save} siempre estampa {@code tenant_id} desde
 * {@link TenantContext#require()}, nunca desde el dominio (que no lo
 * modela) — "los escrituras siempre estampan tenant_id desde el contexto,
 * nunca desde el payload" (decisión de diseño de multi-tenancy).
 */
@Component
public class WorkItemRepositoryAdapter implements WorkItemRepositoryPort {

    private final WorkItemJpaRepository repository;

    public WorkItemRepositoryAdapter(WorkItemJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<WorkItem> findById(WorkItemId id) {
        return repository.findById(Long.parseLong(id.value())).map(WorkItemMapper::toDomain);
    }

    @Override
    public List<WorkItem> findByProjectId(ProjectId projectId) {
        return repository.findByProjectId(Long.parseLong(projectId.value())).stream()
                .map(WorkItemMapper::toDomain)
                .toList();
    }

    @Override
    public WorkItem save(WorkItem workItem) {
        Long tenantId = Long.parseLong(TenantContext.require().value());
        WorkItemJpaEntity saved = repository.save(WorkItemMapper.toEntity(workItem, tenantId));
        return WorkItemMapper.toDomain(saved);
    }
}
