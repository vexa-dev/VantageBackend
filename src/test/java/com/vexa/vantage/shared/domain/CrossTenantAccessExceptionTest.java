package com.vexa.vantage.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de comportamiento para {@link CrossTenantAccessException}: es una
 * excepción en tiempo de ejecución (runtime), y su fábrica produce un
 * mensaje que nombra el recurso infractor y el tenant esperado.
 */
class CrossTenantAccessExceptionTest {

    @Test
    void forResourceBuildsMessageWithResourceTypeIdAndTenant() {
        CrossTenantAccessException exception = CrossTenantAccessException.forResource(
                "Project", "p-42", TenantId.of("tenant-acme"));

        assertThat(exception.getMessage()).isEqualTo("Project 'p-42' does not belong to tenant 'tenant-acme'");
    }

    @Test
    void forResourceReflectsDifferentInputsInTheMessage() {
        CrossTenantAccessException exception = CrossTenantAccessException.forResource(
                "WorkItem", "wi-7", TenantId.of("tenant-globex"));

        assertThat(exception.getMessage()).isEqualTo("WorkItem 'wi-7' does not belong to tenant 'tenant-globex'");
    }

    @Test
    void isARuntimeExceptionNotCheckedException() {
        assertThat(new CrossTenantAccessException("boom")).isInstanceOf(RuntimeException.class);
    }
}
