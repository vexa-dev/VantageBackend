package com.vexa.vantage.identity.application;

/**
 * Se lanza cuando se detecta el reuso de un refresh token ya revocado: se
 * asume que el token fue robado, por lo que {@link RefreshTokenService}
 * revoca preventivamente todos los refresh tokens vigentes del usuario
 * antes de lanzar esta excepción para abortar la rotación en curso.
 *
 * <p>Tipo específico, deliberadamente distinto de {@link SecurityException}
 * genérica: esta señal siempre debe mapear a 401 Unauthorized en la API, sin
 * arriesgar a capturar (y responder igual) cualquier otro
 * {@link SecurityException} no relacionado que pudiera lanzarse en otra
 * parte de la aplicación.
 */
public class RefreshTokenReuseDetectedException extends RuntimeException {

    public RefreshTokenReuseDetectedException(String message) {
        super(message);
    }
}
