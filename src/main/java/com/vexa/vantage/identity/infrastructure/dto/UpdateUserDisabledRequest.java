package com.vexa.vantage.identity.infrastructure.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la solicitud de {@code PATCH /api/v1/users/{id}/disabled}:
 * {@code disabled=true} deshabilita al usuario, {@code disabled=false} lo
 * reactiva.
 */
public record UpdateUserDisabledRequest(

        @NotNull
        Boolean disabled) {
}
