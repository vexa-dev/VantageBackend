package com.vexa.vantage.delivery.infrastructure.dto;

import com.vexa.vantage.delivery.domain.ProjectStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la solicitud de {@code PATCH /api/v1/projects/{id}/status}:
 * {@code ACTIVE} restaura el proyecto, {@code ARCHIVED} lo archiva.
 */
public record UpdateProjectStatusRequest(

        @NotNull
        ProjectStatus status) {
}
