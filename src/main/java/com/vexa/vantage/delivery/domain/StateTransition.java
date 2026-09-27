package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;

import java.util.Set;

/**
 * Una transición declarada entre dos estados dentro de una
 * {@link WorkflowDefinition}, acotada por {@code appliesTo} (tipos de item
 * para los que la transición es válida) exactamente como {@link WorkflowState}.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson.
 *
 * @param from      clave del estado de origen
 * @param to        clave del estado de destino
 * @param appliesTo tipos de item para los que esta transición es válida; nunca vacío
 */
public record StateTransition(String from, String to, Set<String> appliesTo) {

    public StateTransition {
        DomainValidationException.requireNonBlank(from, "StateTransition.from");
        DomainValidationException.requireNonBlank(to, "StateTransition.to");
        if (appliesTo == null || appliesTo.isEmpty()) {
            throw new DomainValidationException("StateTransition.appliesTo must not be null or empty");
        }
        appliesTo = Set.copyOf(appliesTo);
    }

    /**
     * Indica si esta transición declarada cubre exactamente el
     * {@code itemType}/{@code from}/{@code to} solicitado.
     */
    boolean matches(String itemType, String fromState, String toState) {
        return appliesTo.contains(itemType) && from.equals(fromState) && to.equals(toState);
    }
}
