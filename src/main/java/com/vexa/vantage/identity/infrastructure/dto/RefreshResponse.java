package com.vexa.vantage.identity.infrastructure.dto;

/**
 * Cuerpo de la respuesta de {@code POST /api/v1/auth/refresh}: el nuevo
 * token de acceso emitido tras rotar el refresh token. El refresh token
 * rotado se re-envía exclusivamente vía la cookie {@code refresh_token},
 * nunca en este cuerpo JSON.
 */
public record RefreshResponse(String accessToken, String tokenType) {

    public RefreshResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
