package com.vexa.vantage.identity.application;

/**
 * Se lanza cuando se presenta un refresh token cuyo {@code jti} no es
 * conocido por el repositorio: no fue emitido por este sistema, o su
 * registro ya fue purgado.
 *
 * <p>Tipo específico de {@link RefreshTokenService}, deliberadamente
 * distinto de {@link IllegalArgumentException} genérica: un {@code jti}
 * desconocido es una falla de autenticación (debe mapear a 401 Unauthorized
 * en la API), no un argumento inválido cualquiera que pudiera originarse en
 * otra parte de la aplicación.
 */
public class RefreshTokenNotFoundException extends RuntimeException {

    public RefreshTokenNotFoundException(String message) {
        super(message);
    }
}
