package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;

/**
 * Objeto de valor que identifica una {@link WorkflowDefinition}.
 *
 * <p>No vive en {@code shared/domain} porque, a diferencia de
 * {@code ProjectId}/{@code WorkItemId}, ningún otro contexto delimitado
 * referencia una definición de workflow directamente — la referencia
 * {@code project.workflow_definition_id} es interna al contexto de
 * delivery.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public record WorkflowDefinitionId(String value) {

    public WorkflowDefinitionId {
        DomainValidationException.requireNonBlank(value, "WorkflowDefinitionId");
    }

    public static WorkflowDefinitionId of(String value) {
        return new WorkflowDefinitionId(value);
    }
}
