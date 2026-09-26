package com.vexa.vantage.shared.domain;

/**
 * Objeto de valor que identifica a un usuario en todos los contextos
 * delimitados (bounded contexts).
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}). La construcción solo es posible a través
 * de la fábrica {@link #of(String)}, que rechaza valores {@code null} o en
 * blanco.
 */
public record UserId(String value) {

    public UserId {
        DomainValidationException.requireNonBlank(value, "UserId");
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}
