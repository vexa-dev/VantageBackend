package com.vexa.vantage.shared.infrastructure;

import com.vexa.vantage.shared.domain.CrossTenantAccessException;
import com.vexa.vantage.shared.domain.DomainValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Traduce las excepciones de dominio del núcleo compartido y los fallos de
 * Bean Validation a un cuerpo de respuesta {@link ApiError} uniforme.
 *
 * <ul>
 *     <li>{@link DomainValidationException} → 400 Bad Request</li>
 *     <li>{@link CrossTenantAccessException} → 404 Not Found (un recurso de
 *     otro tenant debe aparecer como inexistente, no como prohibido)</li>
 *     <li>{@link MethodArgumentNotValidException} (Bean Validation sobre
 *     {@code @RequestBody}) → 400 Bad Request con un detalle de campo por
 *     cada violación</li>
 * </ul>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ApiError> handleDomainValidation(DomainValidationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(CrossTenantAccessException.class)
    public ResponseEntity<ApiError> handleCrossTenantAccess(CrossTenantAccessException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException exception) {
        List<ApiError.FieldViolation> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldViolation)
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.ofFieldErrors(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors));
    }

    private ApiError.FieldViolation toFieldViolation(FieldError fieldError) {
        return new ApiError.FieldViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
