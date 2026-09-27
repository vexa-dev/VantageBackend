package com.vexa.vantage.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Spring Data para {@link ProjectMembershipJpaEntity}, usado
 * por {@link ProjectMembershipRepositoryAdapter} para implementar
 * {@code ProjectMembershipRepositoryPort} (fase 3.35).
 */
public interface ProjectMembershipJpaRepository extends JpaRepository<ProjectMembershipJpaEntity, Long> {

    List<ProjectMembershipJpaEntity> findByProjectId(Long projectId);

    Optional<ProjectMembershipJpaEntity> findByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMembershipJpaEntity> findByUserId(Long userId);
}
