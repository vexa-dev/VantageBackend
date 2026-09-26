package com.vexa.vantage.shared.domain;

/**
 * Se lanza cuando una operación intenta acceder a un recurso que pertenece
 * a un tenant distinto del que está actualmente en contexto.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson.
 * {@code ApiExceptionHandler} mapea esto a HTTP 404 para que los recursos de
 * otro tenant aparezcan como inexistentes en lugar de revelar su presencia
 * mediante un 403.
 */
public class CrossTenantAccessException extends RuntimeException {

    public CrossTenantAccessException(String message) {
        super(message);
    }

    public static CrossTenantAccessException forResource(String resourceType, String resourceId, TenantId expectedTenant) {
        return new CrossTenantAccessException(
                "%s '%s' does not belong to tenant '%s'".formatted(resourceType, resourceId, expectedTenant.value()));
    }
}
