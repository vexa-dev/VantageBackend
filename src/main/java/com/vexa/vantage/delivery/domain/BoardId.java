package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;

/**
 * Objeto de valor que identifica un {@link Board}.
 *
 * <p>No vive en {@code shared/domain} porque, igual que
 * {@link WorkflowDefinitionId}, ningún otro contexto delimitado referencia
 * un tablero directamente.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public record BoardId(String value) {

    public BoardId {
        DomainValidationException.requireNonBlank(value, "BoardId");
    }

    public static BoardId of(String value) {
        return new BoardId(value);
    }
}
