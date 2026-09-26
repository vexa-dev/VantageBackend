package com.vexa.vantage.identity.infrastructure.dto;

/**
 * Cuerpo de la respuesta de {@code POST /api/v1/auth/login}: solo el token
 * de acceso. El refresh token viaja exclusivamente en la cookie
 * {@code refresh_token} ({@code HttpOnly;Secure;SameSite=Strict}), nunca en
 * este cuerpo JSON.
 */
public record LoginResponse(String accessToken, String tokenType) {

    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
