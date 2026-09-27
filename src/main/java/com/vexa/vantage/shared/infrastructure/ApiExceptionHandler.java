package com.vexa.vantage.shared.infrastructure;

import com.vexa.vantage.delivery.application.ProjectNotFoundException;
import com.vexa.vantage.delivery.application.WorkItemNotFoundException;
import com.vexa.vantage.delivery.application.WorkflowDefinitionNotFoundException;
import com.vexa.vantage.delivery.domain.IllegalTransitionException;
import com.vexa.vantage.identity.application.InvalidCredentialsException;
import com.vexa.vantage.identity.application.RefreshTokenNotFoundException;
import com.vexa.vantage.identity.application.RefreshTokenReuseDetectedException;
import com.vexa.vantage.identity.application.UserNotFoundException;
import com.vexa.vantage.identity.domain.InsufficientPermissionException;
import com.vexa.vantage.shared.domain.CrossTenantAccessException;
import com.vexa.vantage.shared.domain.DomainValidationException;
import io.jsonwebtoken.JwtException;
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
 *     <li>{@link InsufficientPermissionException} → 403 Forbidden (el actor
 *     existe y está autenticado, pero su rol no alcanza)</li>
 *     <li>{@link InvalidCredentialsException} → 401 Unauthorized (login con
 *     email/contraseña incorrectos, o usuario deshabilitado)</li>
 *     <li>{@link JwtException} → 401 Unauthorized (token de acceso o de
 *     refresh malformado, con firma inválida, o expirado)</li>
 *     <li>{@link RefreshTokenReuseDetectedException} → 401 Unauthorized
 *     (reuso detectado de un refresh token ya revocado)</li>
 *     <li>{@link RefreshTokenNotFoundException} → 401 Unauthorized (un
 *     {@code jti} de refresh token desconocido)</li>
 *     <li>{@link UserNotFoundException} → 404 Not Found (el usuario
 *     objetivo de una operación de administración no existe, o no pertenece
 *     al tenant vigente)</li>
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

    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<ApiError> handleInsufficientPermission(InsufficientPermissionException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(HttpStatus.FORBIDDEN, exception.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(HttpStatus.UNAUTHORIZED, exception.getMessage()));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwtException(JwtException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
    }

    // Nota de diseño: deliberadamente NO se mapean acá los tipos genéricos
    // del JDK `SecurityException`/`IllegalArgumentException`. Cualquier
    // código no relacionado con autenticación (validaciones de otros
    // bounded contexts, bugs de argumentos) puede lanzarlos, y un mapeo
    // global a 401 les asignaría incorrectamente un significado de
    // autenticación que no tienen. Cada handler de abajo captura solo el
    // tipo específico que `RefreshTokenService` efectivamente lanza.

    @ExceptionHandler(RefreshTokenReuseDetectedException.class)
    public ResponseEntity<ApiError> handleRefreshTokenReuseDetected(RefreshTokenReuseDetectedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(HttpStatus.UNAUTHORIZED, exception.getMessage()));
    }

    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<ApiError> handleRefreshTokenNotFound(RefreshTokenNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(HttpStatus.UNAUTHORIZED, exception.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiError> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(WorkItemNotFoundException.class)
    public ResponseEntity<ApiError> handleWorkItemNotFound(WorkItemNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(WorkflowDefinitionNotFoundException.class)
    public ResponseEntity<ApiError> handleWorkflowDefinitionNotFound(WorkflowDefinitionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    /**
     * 400, no 409: decisión ya registrada en el Javadoc de
     * {@link IllegalTransitionException} desde Phase 4 batch 1 ("se mapea a
     * HTTP 400"), anterior a esta fase — se mantiene esa decisión existente
     * en lugar de introducir 409 para no contradecir un diseño ya
     * documentado (el spec no exige un código HTTP concreto para esta
     * excepción).
     */
    @ExceptionHandler(IllegalTransitionException.class)
    public ResponseEntity<ApiError> handleIllegalTransition(IllegalTransitionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(HttpStatus.BAD_REQUEST, exception.getMessage()));
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
