package com.vexa.vantage.delivery.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de Spring Data para {@link BoardJpaEntity}, usado por
 * {@link BoardRepositoryAdapter} para implementar
 * {@code delivery.application.BoardRepositoryPort}.
 */
public interface BoardJpaRepository extends JpaRepository<BoardJpaEntity, Long> {

    Optional<BoardJpaEntity> findByProjectId(Long projectId);
}
