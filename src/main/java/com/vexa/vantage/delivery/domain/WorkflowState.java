package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;

import java.util.Set;

/**
 * Un estado declarado dentro de una {@link WorkflowDefinition}.
 *
 * <p>{@code appliesTo} es el conjunto de claves de tipo de item (por
 * ejemplo {@code "EPIC"}, {@code "TASK"}) para las que este estado es
 * válido — es lo que permite que EPIC/STORY y TASK convivan en una sola
 * definición de workflow con vocabularios de estado distintos (adenda de
 * reconciliación de diseño: "unified state model").
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson.
 *
 * @param key        clave estable del estado (por ejemplo {@code "TODO"})
 * @param name       nombre para mostrar
 * @param category   categoría amplia del estado (por ejemplo
 *                   {@code "TODO"}/{@code "IN_PROGRESS"}/{@code "DONE"}/{@code "BLOCKED"})
 * @param order      orden de presentación dentro de su categoría/tablero
 * @param appliesTo  tipos de item para los que este estado es válido; nunca vacío
 */
public record WorkflowState(String key, String name, String category, int order, Set<String> appliesTo) {

    public WorkflowState {
        DomainValidationException.requireNonBlank(key, "WorkflowState.key");
        DomainValidationException.requireNonBlank(name, "WorkflowState.name");
        DomainValidationException.requireNonBlank(category, "WorkflowState.category");
        if (appliesTo == null || appliesTo.isEmpty()) {
            throw new DomainValidationException("WorkflowState.appliesTo must not be null or empty");
        }
        appliesTo = Set.copyOf(appliesTo);
    }
}
