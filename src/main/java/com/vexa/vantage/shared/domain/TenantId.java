package com.vexa.vantage.shared.domain;

/**
 * Objeto de valor que identifica a un tenant en todos los contextos
 * delimitados (bounded contexts).
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}). La construcción solo es posible a través
 * de la fábrica {@link #of(String)}, que rechaza valores {@code null} o en
 * blanco.
 */
public record TenantId(String value) {

    public TenantId {
        DomainValidationException.requireNonBlank(value, "TenantId");
    }

    public static TenantId of(String value) {
        return new TenantId(value);
    }
}
