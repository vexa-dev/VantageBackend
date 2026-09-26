package com.vexa.vantage.identity.domain;

/**
 * Se lanza cuando un actor intenta una operación para la que su rol
 * (a nivel de proyecto o de tenant) resulta insuficiente.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson. Análoga a
 * {@code CrossTenantAccessException} del núcleo compartido, pero para el
 * caso de "rol insuficiente" en lugar de "recurso de otro tenant".
 * {@code ApiExceptionHandler} mapea esto a HTTP 403 Forbidden.
 */
public class InsufficientPermissionException extends RuntimeException {

    public InsufficientPermissionException(String message) {
        super(message);
    }
}
