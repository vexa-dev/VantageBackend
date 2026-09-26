package com.vexa.vantage.shared.infrastructure;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo de error uniforme devuelto por {@link ApiExceptionHandler} para
 * cada excepción mapeada, de modo que los clientes de la API puedan confiar
 * en una única forma consistente.
 */
public record ApiError(int status, String error, String message, Instant timestamp, List<FieldViolation> fieldErrors) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String message) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, Instant.now(), List.of());
    }

    public static ApiError ofFieldErrors(HttpStatus status, String message, List<FieldViolation> fieldErrors) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, Instant.now(), fieldErrors);
    }
}
