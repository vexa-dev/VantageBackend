package com.vexa.vantage.identity.application;

/**
 * Se lanza cuando se busca un {@link com.vexa.vantage.identity.domain.AppUser}
 * por identificador y no existe (o no pertenece al tenant vigente en
 * {@code TenantContext}, en cuyo caso el filtro Hibernate de tenant ya lo
 * hace invisible para la consulta).
 *
 * <p>Tipo específico, deliberadamente distinto de {@link IllegalArgumentException}
 * genérica: un usuario objetivo inexistente es un recurso no encontrado (debe
 * mapear a 404 Not Found en la API), no un argumento inválido cualquiera que
 * pudiera originarse en otra parte de la aplicación.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
