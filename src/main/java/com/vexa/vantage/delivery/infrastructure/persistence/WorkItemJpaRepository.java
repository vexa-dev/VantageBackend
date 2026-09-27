package com.vexa.vantage.delivery.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de Spring Data para {@link WorkItemJpaEntity}, usado por
 * {@link WorkItemRepositoryAdapter} para implementar
 * {@code delivery.application.WorkItemRepositoryPort}.
 */
public interface WorkItemJpaRepository extends JpaRepository<WorkItemJpaEntity, Long> {

    List<WorkItemJpaEntity> findByProjectId(Long projectId);
}
