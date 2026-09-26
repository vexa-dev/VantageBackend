package com.vexa.vantage.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato de comportamiento para {@link TenantId}: igualdad de valor,
 * consistencia de hashCode y rechazo de valores brutos null/en blanco.
 */
class TenantIdTest {

    @Test
    void twoTenantIdsWithTheSameValueAreEqualAndShareHashCode() {
        TenantId first = TenantId.of("11111111-1111-1111-1111-111111111111");
        TenantId second = TenantId.of("11111111-1111-1111-1111-111111111111");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void tenantIdsWithDifferentValuesAreNotEqual() {
        TenantId first = TenantId.of("11111111-1111-1111-1111-111111111111");
        TenantId second = TenantId.of("22222222-2222-2222-2222-222222222222");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void exposesTheRawValuePassedToTheFactory() {
        TenantId tenantId = TenantId.of("33333333-3333-3333-3333-333333333333");

        assertThat(tenantId.value()).isEqualTo("33333333-3333-3333-3333-333333333333");
    }

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> TenantId.of(null));
    }

    @Test
    void rejectsBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> TenantId.of("   "));
    }
}
