package com.vexa.vantage.identity.application;

/**
 * Resultado de un login exitoso: el par de tokens emitidos para la sesión.
 */
public record AuthenticationResult(String accessToken, String refreshToken) {
}
