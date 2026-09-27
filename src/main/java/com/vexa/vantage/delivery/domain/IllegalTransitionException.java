package com.vexa.vantage.delivery.domain;

/**
 * Se lanza cuando se intenta una transición de estado que la
 * {@link WorkflowDefinition} activa no declara para el tipo de item
 * involucrado (independientemente de si la transición existe para otro
 * tipo de item).
 *
 * <p>Tipo propio y con nombre semántico (no una excepción JDK genérica),
 * siguiendo la regla fijada en la Fase 3: cada condición de falla nueva
 * recibe su propia excepción tipada para que {@code ApiExceptionHandler}
 * (donde se mapea a HTTP 400) no termine clasificando mal un tipo genérico
 * reutilizado por código futuro no relacionado.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson.
 */
public class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(String message) {
        super(message);
    }
}
