package com.vexa.vantage.identity.infrastructure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de la solicitud de {@code POST /api/v1/auth/login}.
 */
public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password) {
}
