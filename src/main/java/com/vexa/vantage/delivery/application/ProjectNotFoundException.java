package com.vexa.vantage.delivery.application;

/**
 * Se lanza cuando se busca un {@link com.vexa.vantage.delivery.domain.Project}
 * por identificador y no existe (o no pertenece al tenant vigente, en cuyo
 * caso el filtro de tenant ya lo hace invisible para la consulta).
 *
 * <p>Tipo específico, deliberadamente distinto de {@link IllegalArgumentException}
 * genérica: un proyecto objetivo inexistente es un recurso no encontrado (debe
 * mapear a 404 Not Found en la API), no un argumento inválido cualquiera que
 * pudiera originarse en otra parte de la aplicación.
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String message) {
        super(message);
    }
}
