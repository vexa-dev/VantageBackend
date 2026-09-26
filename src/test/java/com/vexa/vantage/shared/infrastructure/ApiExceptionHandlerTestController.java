package com.vexa.vantage.shared.infrastructure;

import com.vexa.vantage.shared.domain.CrossTenantAccessException;
import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.TenantId;
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

    @PostMapping("/bean-validation")
    void triggerBeanValidation(@Valid @RequestBody Payload payload) {
        // inalcanzable cuando falla la validación
    }

    record Payload(@NotBlank(message = "name must not be blank") String name) {
    }
}
