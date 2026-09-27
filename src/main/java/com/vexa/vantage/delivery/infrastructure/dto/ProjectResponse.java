package com.vexa.vantage.delivery.infrastructure.dto;

import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.ProjectStatus;

/**
 * Representación de un proyecto expuesta por {@code ProjectController}. Se
 * mapea manualmente desde {@link Project} (dominio puro, sin dependencia de
 * Jackson) en lugar de serializar el objeto de dominio directamente.
 */
public record ProjectResponse(
        String id,
        String tenantId,
        String name,
        String description,
        String workflowDefinitionId,
        ProjectStatus status) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.id().value(),
                project.tenantId().value(),
                project.name(),
                project.description(),
                project.workflowDefinitionId().value(),
                project.status());
    }
}
