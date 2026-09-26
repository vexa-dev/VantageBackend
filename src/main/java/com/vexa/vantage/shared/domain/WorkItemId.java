package com.vexa.vantage.shared.domain;

/**
 * Objeto de valor que identifica un elemento de trabajo (work item) (épica,
 * historia, incidencia, tarea, ...) en todos los contextos delimitados
 * (bounded contexts).
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}). La construcción solo es posible a través
 * de la fábrica {@link #of(String)}, que rechaza valores {@code null} o en
 * blanco.
 */
public record WorkItemId(String value) {

    public WorkItemId {
        DomainValidationException.requireNonBlank(value, "WorkItemId");
    }

    public static WorkItemId of(String value) {
        return new WorkItemId(value);
    }
}
