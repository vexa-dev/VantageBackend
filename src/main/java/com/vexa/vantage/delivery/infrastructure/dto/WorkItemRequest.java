package com.vexa.vantage.delivery.infrastructure.dto;

import com.vexa.vantage.delivery.domain.TShirtSize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de la solicitud de {@code POST /api/v1/projects/{id}/work-items}:
 * los datos para crear un work item. El {@code projectId} viaja en la ruta,
 * no en el cuerpo; {@code actorId} se resuelve del actor autenticado (JWT).
 *
 * <p>Requiere {@code type}/{@code state} explícitos (spec, requisito
 * "Generic Work Item Representation", escenario "Creating a work item in a
 * project": "a user creates a new work item with a valid type and initial
 * state") — esta capa no infiere el estado inicial desde la
 * {@code WorkflowDefinition} del proyecto.
 */
public record WorkItemRequest(

        @NotBlank
        String type,

        @NotBlank
        String state,

        @NotBlank
        String title,

        String description,

        @Min(0) @Max(100)
        Integer businessValue,

        @Min(0) @Max(100)
        Integer urgency,

        @Min(0)
        Integer storyPoints,

        TShirtSize tshirtSize) {
}
