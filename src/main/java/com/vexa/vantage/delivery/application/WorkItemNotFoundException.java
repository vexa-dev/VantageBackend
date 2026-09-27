package com.vexa.vantage.delivery.application;

/**
 * Se lanza cuando se busca un {@link com.vexa.vantage.delivery.domain.WorkItem}
 * por identificador y no existe (o no pertenece al tenant vigente, en cuyo
 * caso el filtro de tenant ya lo hace invisible para la consulta).
 *
 * <p>Tipo específico, deliberadamente distinto de {@link IllegalArgumentException}
 * genérica: un work item objetivo inexistente es un recurso no encontrado
 * (debe mapear a 404 Not Found en la API), no un argumento inválido cualquiera.
 */
public class WorkItemNotFoundException extends RuntimeException {

    public WorkItemNotFoundException(String message) {
        super(message);
    }
}
