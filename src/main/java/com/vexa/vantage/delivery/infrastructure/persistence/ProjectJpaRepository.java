package com.vexa.vantage.delivery.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de Spring Data para {@link ProjectJpaEntity}, usado por
 * {@link ProjectRepositoryAdapter} para implementar
 * {@code delivery.application.ProjectRepositoryPort}.
 */
public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, Long> {
}
