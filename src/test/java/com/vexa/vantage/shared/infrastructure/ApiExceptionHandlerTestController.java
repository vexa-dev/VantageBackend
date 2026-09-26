package com.vexa.vantage.shared.infrastructure;

import com.vexa.vantage.identity.application.InvalidCredentialsException;
import com.vexa.vantage.identity.application.RefreshTokenNotFoundException;
import com.vexa.vantage.identity.application.RefreshTokenReuseDetectedException;
import com.vexa.vantage.identity.application.UserNotFoundException;
import com.vexa.vantage.identity.domain.AuthorizationPolicy;
import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.shared.domain.CrossTenantAccessException;
import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.TenantId;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de fixture exclusivo para pruebas, usado para ejercitar
 * {@link ApiExceptionHandler} de extremo a extremo a través de un despacho
 * real de Spring MVC, sin depender de ningún controlador de dominio real.
 */
@RestController
@RequestMapping("/test/api-exception-handler")
class ApiExceptionHandlerTestController {

    @PostMapping("/domain-validation")
    void triggerDomainValidation() {
        throw new DomainValidationException("raw value must not be null or blank");
    }

    @PostMapping("/cross-tenant-access")
    void triggerCrossTenantAccess() {
        throw CrossTenantAccessException.forResource("Project", "p-1", TenantId.of("tenant-acme"));
    }

    @PostMapping("/insufficient-permission")
    void triggerInsufficientPermission() {
        AuthorizationPolicy.requireTenantAdmin(TenantRole.MEMBER);
    }

    @PostMapping("/bean-validation")
    void triggerBeanValidation(@Valid @RequestBody Payload payload) {
        // inalcanzable cuando falla la validación
    }

    @PostMapping("/invalid-credentials")
    void triggerInvalidCredentials() {
        throw new InvalidCredentialsException("Invalid email or password");
    }

    @PostMapping("/jwt-exception")
    void triggerJwtException() {
        throw new SignatureException("JWT signature does not match");
    }

    @PostMapping("/refresh-token-reuse")
    void triggerRefreshTokenReuseDetected() {
        throw new RefreshTokenReuseDetectedException("Refresh token reuse detected for user 'user-1'");
    }

    @PostMapping("/unknown-jti")
    void triggerRefreshTokenNotFound() {
        throw new RefreshTokenNotFoundException("Unknown refresh token jti: abc-123");
    }

    @PostMapping("/unknown-user")
    void triggerUserNotFound() {
        throw new UserNotFoundException("User not found: user-1");
    }

    record Payload(@NotBlank(message = "name must not be blank") String name) {
    }
}
