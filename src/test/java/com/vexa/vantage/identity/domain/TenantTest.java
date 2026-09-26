package com.vexa.vantage.identity.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.TenantId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica el invariante de dominio de {@link Tenant}: el nombre no puede
 * estar vacío ni en blanco.
 */
class TenantTest {

    @Test
    void createsTenantWhenNameIsPresent() {
        Tenant tenant = new Tenant(TenantId.of("tenant-1"), "Acme Corp");

        assertThat(tenant.id()).isEqualTo(TenantId.of("tenant-1"));
        assertThat(tenant.name()).isEqualTo("Acme Corp");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Tenant(TenantId.of("tenant-1"), "   "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Tenant.name");
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new Tenant(TenantId.of("tenant-1"), null))
                .isInstanceOf(DomainValidationException.class);
    }
}
