package com.vexa.vantage.delivery.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de Spring Data para {@link WorkflowDefinitionJpaEntity}, usado
 * por {@link WorkflowDefinitionRepositoryAdapter} para implementar
 * {@code delivery.application.WorkflowDefinitionRepositoryPort}.
 */
public interface WorkflowDefinitionJpaRepository extends JpaRepository<WorkflowDefinitionJpaEntity, Long> {

    Optional<WorkflowDefinitionJpaEntity> findByBuiltInTemplateTrue();
}
