package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.application.ProjectRepositoryPort;
import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.shared.domain.ProjectId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter (arquitectura hexagonal) que implementa {@link ProjectRepositoryPort}
 * sobre JPA/Spring Data. Su nombre termina en {@code RepositoryAdapter} y vive
 * en {@code infrastructure.persistence} a propósito, para que
 * {@code TenantFilterAspect} lo intercepte automáticamente.
 */
@Component
public class ProjectRepositoryAdapter implements ProjectRepositoryPort {

    private final ProjectJpaRepository repository;

    public ProjectRepositoryAdapter(ProjectJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Project> findById(ProjectId id) {
        return repository.findById(Long.parseLong(id.value())).map(ProjectMapper::toDomain);
    }

    @Override
    public List<Project> findByIds(List<ProjectId> ids) {
        List<Long> rawIds = ids.stream().map(id -> Long.parseLong(id.value())).toList();
        return repository.findAllById(rawIds).stream().map(ProjectMapper::toDomain).toList();
    }

    @Override
    public Project save(Project project) {
        ProjectJpaEntity saved = repository.save(ProjectMapper.toEntity(project));
        return ProjectMapper.toDomain(saved);
    }
}
