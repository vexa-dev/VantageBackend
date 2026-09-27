package com.vexa.vantage.delivery.application;

/**
 * Se lanza cuando se busca una
 * {@link com.vexa.vantage.delivery.domain.WorkflowDefinition} por
 * identificador (o la plantilla built-in) y no existe.
 *
 * <p>Tipo específico, deliberadamente distinto de {@link IllegalArgumentException}
 * genérica: una definición de workflow inexistente es un recurso no
 * encontrado (debe mapear a 404 Not Found en la API), no un argumento
 * inválido cualquiera que pudiera originarse en otra parte de la aplicación.
 */
public class WorkflowDefinitionNotFoundException extends RuntimeException {

    public WorkflowDefinitionNotFoundException(String message) {
        super(message);
    }
}
