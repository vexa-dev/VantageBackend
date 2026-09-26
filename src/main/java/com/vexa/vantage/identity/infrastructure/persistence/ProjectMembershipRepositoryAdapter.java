package com.vexa.vantage.identity.infrastructure.persistence;

import com.vexa.vantage.identity.application.ProjectMembershipRepositoryPort;
import com.vexa.vantage.identity.domain.ProjectMembership;
import com.vexa.vantage.shared.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter (arquitectura hexagonal) que implementa
 * {@link ProjectMembershipRepositoryPort} sobre JPA/Spring Data.
 *
 * <p>{@code project_membership} no tiene columna de tenant propia (la
 * pertenencia a un tenant se deriva transitivamente del proyecto, cuyo
 * contexto delimitado de delivery/colaboración todavía no existe), por lo
 * que {@code TenantFilterAspect} habilita el filtro Hibernate
 * {@code tenantFilter} alrededor de este adapter (por convención de nombre y
 * paquete) sin efecto real: {@link ProjectMembershipJpaEntity} no declara
 * {@code @Filter} para ese nombre.
 */
@Component
public class ProjectMembershipRepositoryAdapter implements ProjectMembershipRepositoryPort {

    private final ProjectMembershipJpaRepository repository;

    public ProjectMembershipRepositoryAdapter(ProjectMembershipJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProjectMembership> findByProjectId(String projectId) {
        return repository.findByProjectId(Long.parseLong(projectId)).stream()
                .map(ProjectMembershipMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProjectMembership> findByProjectIdAndUserId(String projectId, UserId userId) {
        return repository.findByProjectIdAndUserId(Long.parseLong(projectId), Long.parseLong(userId.value()))
                .map(ProjectMembershipMapper::toDomain);
    }

    @Override
    public void save(ProjectMembership membership) {
        repository.save(ProjectMembershipMapper.toEntity(membership));
    }
}
