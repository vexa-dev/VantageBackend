package com.vexa.vantage.identity.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.TenantId;

/**
 * Entidad de dominio que representa un tenant (organización) dentro del
 * contexto delimitado de identidad.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}). El único invariante impuesto aquí es que el
 * nombre del tenant no puede estar vacío ni en blanco.
 */
public final class Tenant {

    private final TenantId id;
    private final String name;

    public Tenant(TenantId id, String name) {
        this.id = id;
        this.name = DomainValidationException.requireNonBlank(name, "Tenant.name");
    }

    public TenantId id() {
        return id;
    }

    public String name() {
        return name;
    }
}
